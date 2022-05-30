package me.blvckbytes.blvcksys.handlers.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/23/2022

  Draws animations frame by frame, where usually the GUI is locked while
  drawing frames and the animator just moves raw items around, without accounting
  for any events, pagination or the like. There's always a transition from one
  inventory to another, but if the destination is null, the source becomes the
  destination and the source becomes a null-filled array. This way, both cases can
  be animated using the same routines.
*/
public class GuiAnimation {

  // How many ticks each animation frame may take up
  private static final long TICKS_PER_FRAME = 1;

  private final AnimationType animation;
  private final JavaPlugin plugin;
  private final int numFrames, numRows;
  private final Inventory inv;
  private final Runnable done, ready;
  private final ItemStack[] fromContents, toContents;
  private final AtomicBoolean fastForwarded;
  private final List<Integer> mask;
  private final ItemStack filler;

  private int currFrame;

  /**
   * Create a new animation for a GUI
   * @param plugin JavaPlugin ref, used for scheduling
   * @param animation Animation to play
   * @param fromContents Items to animate from
   * @param toContents Items to animate to
   * @param inv Inventory to animate from and to contents into
   * @param mask List of slots to animate, leave at null to animate all slots
   * @param filler Filler item used when the inventories are unequal in size
   * @param ready Ready callback, signals that the GUI may be presented by now
   * @param done Completion callback, signals that the animation is complete
   */
  public GuiAnimation(
    JavaPlugin plugin,
    AnimationType animation,
    @Nullable ItemStack[] fromContents,
    ItemStack[] toContents,
    Inventory inv,
    @Nullable List<Integer> mask,
    @Nullable ItemStack filler,
    Runnable ready,
    Runnable done
  ) {
    this.plugin = plugin;
    this.ready = ready;
    this.done = done;
    this.animation = animation;
    this.fromContents = fromContents == null ? new ItemStack[inv.getSize()] : fromContents;
    this.toContents = toContents;
    this.inv = inv;
    this.mask = mask;
    this.filler = filler;

    this.numRows = inv.getSize() / 9;
    this.numFrames = getNumFrames();
    this.fastForwarded = new AtomicBoolean(false);

    play();
  }

  /**
   * Plays the current frame, also used to bootstrap playing
   */
  private void play() {
    // Animation has been quit manually already
    if (this.fastForwarded.get())
      return;

    // Copy over the previous items before the first frame plays
    if (currFrame == 0) {
      // But only if there's a transition
      if (fromContents != null) {
        for (int i = 0; i < fromContents.length; i++) {
          if (mask == null || mask.contains(i))
            setItem(i, getItem(fromContents, i));
        }
      }

      ready.run();
    }

    animate();
    nextFrame();
  }

  /**
   * Perform the current frame's animation
   */
  private void animate() {
    switch (animation) {
      // Drawing columns
      case SLIDE_RIGHT, SLIDE_LEFT -> {

        for (int drawCol = 0; drawCol < 9; drawCol++) {
          ItemStack[] origin;
          int readCol;

          if (animation == AnimationType.SLIDE_LEFT) {
            if (drawCol < (numFrames - currFrame - 1)) {
              origin = fromContents;
              readCol = drawCol + currFrame + 1;
            } else {
              origin = toContents;
              readCol = drawCol - (8 - currFrame);
            }
          }

          else {
            if (drawCol > currFrame) {
              origin = fromContents;
              readCol = drawCol - currFrame - 1;
            }
            else {
              origin = toContents;
              readCol = 8 - currFrame + drawCol;
            }
          }

          for (int i = 0; i < numRows * 9; i += 9) {
            if (mask == null || (mask.contains(drawCol + i) && mask.contains(readCol + i)))
              setItem(drawCol + i, getItem(origin, readCol + i));
          }
        }
      }

      // Drawing rows
      case SLIDE_DOWN, SLIDE_UP -> {
        for (int drawRow = 0; drawRow < numRows; drawRow++) {
          ItemStack[] origin;
          int readRow;

          if (animation == AnimationType.SLIDE_DOWN) {
            if (drawRow > currFrame) {
              origin = fromContents;
              readRow = drawRow - (currFrame + 1);
            } else {
              origin = toContents;
              readRow = drawRow + (numRows - currFrame - 1);
            }
          }

          else {
            if (drawRow < (numFrames - currFrame - 1)) {
              origin = fromContents;
              readRow = drawRow + (currFrame + 1);
            } else {
              origin = toContents;
              readRow = drawRow - (numRows - currFrame - 1);
            }
          }

          for (int i = 0; i < 9; i++) {
            if (mask == null || (mask.contains(drawRow * 9 + i) && mask.contains(readRow * 9 + i)))
              setItem(drawRow * 9 + i, getItem(origin, readRow * 9 + i));
          }
        }
      }
    }
  }

  /**
   * Invoke playing the next frame by calling {@link #play()} after the
   * ticks per frame have elapsed, or call the callback if there's no next frame
   */
  private void nextFrame() {
    // While there are still frames left and the inv is open, invoke another frame timer
    if (++currFrame < numFrames && inv.getViewers().size() > 0)
      Bukkit.getScheduler().runTaskLater(plugin, this::play, TICKS_PER_FRAME);

      // Done, call the callback
    else
      done.run();
  }

  /**
   * Get the number of frames this animation will take
   */
  private int getNumFrames() {
    return switch (animation) {
      // Bottom and top will both take as many frames as there are rows
      case SLIDE_UP, SLIDE_DOWN -> inv.getSize() / 9;

      // Left and right take as many frames as there are horizontal slots
      case SLIDE_RIGHT, SLIDE_LEFT -> 9;
    };
  }

  /**
   * Fast forwards an animation by jumping to it's last frame instantly
   */
  public void fastForward() {
    // No frames left, animation is already over
    if (currFrame >= numFrames)
      return;

    // Directly copy the contents (last frame in all cases)
    for (int i = 0; i < Math.min(fromContents.length, toContents.length); i++) {
      if (mask == null || mask.contains(i))
        setItem(i, getItem(toContents, i));
    }

    done.run();
  }

  /**
   * Gets an item from a content-array or returns the filler
   * when the slot is out of range
   * @param contents Contents to fetch from
   * @param slot Slot to get
   * @return Slot contents or filler
   */
  private ItemStack getItem(ItemStack[] contents, int slot) {
    if (slot >= contents.length)
      return filler;
    return contents[slot];
  }

  /**
   * Sets an item into the animating inventory, skips out
   * of range slots
   * @param slot Slot to set to
   * @param item Item to set
   */
  private void setItem(int slot, ItemStack item) {
    if (slot < inv.getSize())
      inv.setItem(slot, item);
  }
}
