package me.blvckbytes.blvcksys.handlers;

import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/09/2022

  Holds multiple connected item frames which together form a bigger
  canvas on which images can be displayed.
 */
public class ItemFrameGroup {

  // Radius to search in for item-frames when scanning for the main member
  private static final double LOC_SEARCH_RAD = 1.0D;

  private final Map<Player, Color[][]> framebuffers;

  // Width and height in frames of this group
  private int width, height;
  private ItemFrame[][] frameGrid;

  public ItemFrameGroup(Location loc) {
    this.framebuffers = new HashMap<>();

    Tuple<BlockFace, Set<ItemFrame>> ret = findMembers(loc);
    if (ret != null)
      createGrid(ret.a(), ret.b());
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  /**
   * Undo all item frame manipulations and clean up
   */
  public void destroy() {
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        ItemFrame frame = frameGrid[x][y];
        removeRenderer(frame);
      }
    }
  }

  /**
   * Set a custom framebuffer for a given player
   * @param p Player to set the framebuffer for
   * @param framebuffer Framebuffer to display
   */
  public void setFramebuffer(Player p, Color[][] framebuffer) {
    this.framebuffers.put(p, framebuffer);
  }

  /**
   * Clear a custom framebuffer for a given player
   * @param p Player to clear the framebuffer for
   */
  public void clearFramebuffer(Player p) {
    this.framebuffers.remove(p);
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Create a two-dimensional grid of all item frames, as they're
   * located in the real world - just with relative coordinates and a 2D plane
   * @param mountFace Face the members are mounted on
   * @param members Set of members that exist
   */
  private void createGrid(BlockFace mountFace, Set<ItemFrame> members) {
    // No members to loop
    if (members.size() == 0 || mountFace == null) {
      width = height = 0;
      return;
    }

    // Find the min- and max values of all three axies
    double[] minMaxAx = {
      Double.MAX_VALUE, Double.MIN_VALUE,  // min-x, max-x
      Double.MAX_VALUE, Double.MIN_VALUE,  // min-y, max-y
      Double.MAX_VALUE, Double.MIN_VALUE   // min-z, max-z
    };

    // Loop all members
    for (ItemFrame frame : members) {
      Location loc = frame.getLocation();
      double[] cords = { loc.getX(), loc.getY(), loc.getZ() };

      // Loop all min-max array entries
      for (int i = 0; i < 6; i++) {
        boolean even = i % 2 == 0;
        double cord = cords[i / 2];

        if (
          // i is even (first column), search for min
          (even && cord < minMaxAx[i] || minMaxAx[i] == Double.MAX_VALUE) ||

          // i is odd (second column), search for max
          (!even && cord > minMaxAx[i] || minMaxAx[i] == Double.MIN_VALUE)
        )
          minMaxAx[i] = cord;
      }
    }

    // Height is always delta-y
    height = (int) (minMaxAx[3] - minMaxAx[2]) + 1;

    // East or west, width is delta-z
    boolean isZ = mountFace == BlockFace.EAST || mountFace == BlockFace.WEST;
    if (isZ)
      width = (int) (minMaxAx[5] - minMaxAx[4]) + 1;

    // Has to be south or north, width is delta-x
    else
      width = (int) (minMaxAx[1] - minMaxAx[0]) + 1;

    this.frameGrid = new ItemFrame[width][height];

    // Insert all members in their grid-slot
    for (ItemFrame frame : members) {
      Location loc = frame.getLocation();
      // Calculate the relative x- and y-position
      int y = (int) (loc.getY() - minMaxAx[2]);
      int x = (int) (isZ ? (loc.getZ() - minMaxAx[4]) : (loc.getX() - minMaxAx[0]));
      this.frameGrid[x][y] = frame;
      attachRenderer(frame, x, y);
    }
  }

  /**
   * Remove a previously attached renderer from an item frame
   * @param frame Frame to remove from
   */
  private void removeRenderer(ItemFrame frame) {
    ItemStack stack = frame.getItem();
    frame.setItem(null);

    MapMeta meta = (MapMeta) stack.getItemMeta();
    if (meta == null || !meta.hasMapView())
      return;

    MapView view = meta.getMapView();
    if (view == null)
      return;

    view.getRenderers().clear();
  }

  /**
   * Attach a render function to the provided item-frame
   * @param frame Frame to attach to
   */
  private void attachRenderer(ItemFrame frame, int x, int y) {
    ItemStack stack = new ItemStack(Material.FILLED_MAP);
    MapMeta meta = (MapMeta) stack.getItemMeta();
    World w = frame.getLocation().getWorld();

    if (meta == null || w == null)
      return;

    // Create a new map and override it's renderers
    MapView view = Bukkit.createMap(w);
    view.getRenderers().clear();
    view.addRenderer(new MapRenderer(true) {

      @Override
      public void render(@NotNull MapView mv, @NotNull MapCanvas cv, @NotNull Player p) {
        // Get the current player's custom framebuffer
        Color[][] framebuffer = framebuffers.get(p);
        if (framebuffer == null)
          return;

        // Calculate framebuffer offsets based on this frame's coordinates
        // Every frame can display 128 x 128 pixels
        int xOff = x * 128, yOff = y * 128;

        for (int cx = 0; cx <= 127; cx++) {
          for (int cy = 0; cy <= 127; cy++) {
            int fbX = xOff + cx, fbY = yOff + cy;

            // No framebuffer entry for the current pixel, set to transparent
            if (
              framebuffer.length == 0 ||
              framebuffer[0].length == 0 ||
              fbX >= framebuffer.length ||
              fbY >= framebuffer[0].length
            ) {
              cv.setPixel(cx, cy, (byte) 0);
              continue;
            }

            // Set pixel from framebuffer
            cv.setPixel(cx, cy, MapPalette.matchColor(framebuffer[fbX][fbY]));
          }
        }
      }
    });

    // Add this item-stack to the frame
    meta.setMapView(view);
    stack.setItemMeta(meta);
    frame.setItem(stack);
  }

  /**
   * Find all members of this group, specified by the main member
   * item frame. All frames which directly connect to it are within
   * the same group and will be added to the member-list.
   * @param loc Location of the main member
   * @return A Tuple of the BlockFace all members are mounted on and
   * the set of members that have been discovered
   */
  private Tuple<BlockFace, Set<ItemFrame>> findMembers(Location loc) {
    World w = loc.getWorld();
    if (w == null)
      return null;

    // Get the main member of this group
    ItemFrame member = getFrameAt(loc, true).orElse(null);
    if (member == null)
      return null;

    Set<ItemFrame> members = new HashSet<>();

    // Now, check all of it's neighbor blocks for more frames
    members.add(member);
    checkNeighbors(member, members);

    return new Tuple<>(member.getAttachedFace(), members);
  }

  /**
   * Get a item frame entity at a given location
   * @param loc Location the entity resides at
   * @param tolerance Whether to search with radius tolerance
   * @return Optional ItemFrame, empty if there's no frame at that location
   */
  private Optional<ItemFrame> getFrameAt(Location loc, boolean tolerance) {
    if (loc.getWorld() == null)
      return Optional.empty();

    double rad = tolerance ? LOC_SEARCH_RAD : 0;
    return loc.getWorld().getNearbyEntities(loc, rad, rad, rad)
      .stream()
      .filter(e -> e instanceof ItemFrame)
      .map(e -> (ItemFrame) e)
      .findFirst();
  }

  /**
   * Check all four possible neighbors of a given location
   * for item frame entities and call check on found neighbors again, recursively
   * @param center Center position to check the neighbors of
   * @param members Mutable set of already known members
   */
  private void checkNeighbors(ItemFrame center, Set<ItemFrame> members) {
    Location loc = center.getLocation();
    BlockFace attached = center.getAttachedFace();

    // y + = above, y - = below
    Location above = loc.clone().add(0, 1, 0);
    Location below = loc.clone().add(0, -1, 0);
    Location left = null;
    Location right = null;

    // East: z - = left, z + = right
    // West: z + = left, z - = right
    if (attached == BlockFace.EAST || attached == BlockFace.WEST) {
      left = loc.clone().add(0, 0, attached == BlockFace.EAST ? -1 : 1);
      right = loc.clone().add(0, 0, attached == BlockFace.EAST ? 1 : -1);
    }

    // South: x + = left, x - = right
    // North: x - = left, x + = right
    else if (attached == BlockFace.SOUTH || attached == BlockFace.NORTH) {
      left = loc.clone().add(attached == BlockFace.SOUTH ? 1 : -1, 0, 0);
      right = loc.clone().add(attached == BlockFace.SOUTH ? -1 : 1, 0, 0);
    }

    Location[] locs = new Location[] { above, below, left, right };

    // Loop through all four locations
    for (Location check : locs) {
      // No location to check
      if (check == null)
        continue;

      ItemFrame frame = getFrameAt(check, false).orElse(null);

      // No frame at this location, stop here
      if (frame == null)
        continue;

      // Also walk this frame, if it's not yet known
      if (members.add(frame))
        checkNeighbors(frame, members);
    }
  }
}
