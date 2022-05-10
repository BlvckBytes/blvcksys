package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.PlayerSkin;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerTextures;

import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/09/2022

  Holds multiple item frame groups and handles what they display.
 */
@AutoConstruct
public class ImageFrameHandler implements IAutoConstructed, Listener {

  private final List<ItemFrameGroup> groups;
  private final ILogger logger;
  private final JavaPlugin plugin;

  public ImageFrameHandler(
    @AutoInject ILogger logger,
    @AutoInject JavaPlugin plugin
  ) {
    this.logger = logger;
    this.plugin = plugin;
    this.groups = new ArrayList<>();
  }

  @Override
  public void cleanup() {
    for (ItemFrameGroup group : groups)
      group.destroy();
  }

  @Override
  public void initialize() {
    // Hardcoded location for dev phase
    Location loc = new Location(Bukkit.getWorld("world"), 407, 124, -220);
    ItemFrameGroup group = new ItemFrameGroup(loc, logger);
    this.groups.add(group);

    // Each player should see their own skin
    try {
      for (Player t : Bukkit.getOnlinePlayers()) {
        PlayerTextures textures = t.getPlayerProfile().getTextures();
        PlayerSkin skin = new PlayerSkin(textures.getSkin(), textures.getSkinModel());
        group.setFramebuffer(t, skin.getFullRender());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
