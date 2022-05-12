package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.ImageFrameModel;
import me.blvckbytes.blvcksys.persistence.models.ImageFrameType;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.PlayerSkin;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/09/2022

  Holds multiple item frame groups and handles what they display.
 */
@AutoConstruct
public class ImageFrameHandler implements IImageFrameHandler, IAutoConstructed, Listener {

  private final Map<String, ItemFrameGroup> groups;
  private final Map<String, ImageFrameModel> cache;

  private final IPersistence pers;
  private final ILogger logger;
  private final JavaPlugin plugin;

  public ImageFrameHandler(
    @AutoInject ILogger logger,
    @AutoInject IPersistence pers,
    @AutoInject JavaPlugin plugin
  ) {
    this.logger = logger;
    this.pers = pers;
    this.plugin = plugin;

    this.groups = new HashMap<>();
    this.cache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void cleanup() {
    for (ItemFrameGroup group : groups.values())
      group.destroy();

    groups.clear();
  }

  @Override
  public void initialize() {
    // Load all groups from persistence
    loadGroups();
  }

  @Override
  public List<Tuple<ImageFrameModel, ItemFrameGroup>> findNearbyGroups(Location loc, double radius) {
    List<Tuple<ImageFrameModel, ItemFrameGroup>> res = new ArrayList<>();

    for (ItemFrameGroup group : groups.values()) {
      String name = group.getName().toLowerCase();
      if (group.isNear(loc, radius) && cache.containsKey(name))
        res.add(new Tuple<>(cache.get(name), group));
    }

    return res;
  }

  @Override
  public ItemFrame findNearbyFrame(Location loc) {
    World w = loc.getWorld();

    if (w == null)
      return null;

    // Search for item frame entities that are near the target block
    return w.getNearbyEntities(loc, 1, 1, 1)
      .stream()
      .filter(e -> e instanceof ItemFrame)
      .map(e -> (ItemFrame) e)
      .findFirst()
      .orElse(null);
  }

  @Override
  public ItemFrameGroup findGroupByMember(ItemFrame member) {
    for (ItemFrameGroup group : groups.values()) {
      if (group.isMember(member))
        return group;
    }

    return null;
  }

  @Override
  public boolean createGroup(
    OfflinePlayer creator,
    String name,
    Location loc,
    ImageFrameType type,
    @Nullable String resource
  ) throws PersistenceException {
    try {
      // Create the persistent model of the main frame
      ImageFrameModel frame = new ImageFrameModel(name, loc, type, resource, creator);
      pers.store(frame);

      // Load a new group from this frame
      loadGroup(frame);
      return true;
    } catch (DuplicatePropertyException e) {
      return false;
    }
  }

  @Override
  public boolean deleteGroup(String name) throws PersistenceException {
    // Try to delete the group with this name
    boolean ret = pers.delete(new QueryBuilder<>(
      ImageFrameModel.class,
      "name", EqualityOperation.EQ_IC, name
    )) > 0;

    // Destroy the group with this name
    if (ret) {
      groups.remove(name.toLowerCase()).destroy();
      cache.remove(name.toLowerCase());
    }

    return ret;
  }

  @Override
  public boolean reloadGroup(String name) {
    ItemFrameGroup group = groups.get(name.toLowerCase());

    if (group == null)
      return false;

    for (Player t : Bukkit.getOnlinePlayers())
      loadContent(group, t);

    return true;
  }

  //=========================================================================//
  //                                 Listener                                //
  //=========================================================================//

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    for (ItemFrameGroup group : groups.values())
      loadContent(group, e.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    for (ItemFrameGroup group : groups.values())
      group.clearFramebuffer(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Load all existing groups from persistence into their groups
   */
  private void loadGroups() {
    try {
      // Load the model's group
      for (ImageFrameModel model : pers.list(ImageFrameModel.class))
        loadGroup(model);
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  /**
   * Load a frame group from it's persistent model and store the model in cache
   * @param model Model to load from
   */
  private void loadGroup(ImageFrameModel model) {
    // No item frame at the stored location
    ItemFrame frame = findNearbyFrame(model.getLoc());
    if (frame == null)
      return;

    // Is a member of an already existing group
    ItemFrameGroup group = findGroupByMember(frame);
    if (group != null)
      return;

    ItemFrameGroup newGroup = new ItemFrameGroup(
      model.getLoc(),
      model.getName(),
      logger
    );

    cache.put(model.getName().toLowerCase(), model);

    // Load content for all currently online players
    for (Player t : Bukkit.getOnlinePlayers())
      loadContent(newGroup, t);

    groups.put(model.getName().toLowerCase(), newGroup);
  }

  /**
   * Load the custom content for a given frame group for a given player
   * @param group Target frame group
   * @param p Player to display for
   */
  private void loadContent(ItemFrameGroup group, Player p) {
    try {
      ImageFrameModel model = cache.get(group.getName().toLowerCase());

      if (model == null) {
        group.clearFramebuffer(p);
        return;
      }

      String resource = model.getResource();
      if (resource != null) {
        resource = resource
          .replace("{name}", p.getName())
          .replace("{uuid}", p.getUniqueId().toString());
      }

      switch (model.getType()) {
        case PLAYER_SKIN -> {
          PlayerTextures textures = p.getPlayerProfile().getTextures();
          PlayerSkin skin = new PlayerSkin(textures.getSkin(), textures.getSkinModel());
          group.setFramebuffer(p, skin.getFullRender());
        }

        case URL_IMAGE -> {
          if (resource != null)
            group.setFramebuffer(p, ImageIO.read(new URL(resource)));
        }

        case FILE_IMAGE -> {
          if (resource != null) {
            String path = plugin.getDataFolder().getAbsolutePath() + "/imageframes/" + resource;
            group.setFramebuffer(p, ImageIO.read(new File(path)));
          }
        }
      }
    } catch (Exception e) {
      group.clearFramebuffer(p);
      logger.logError(e);
    }
  }
}
