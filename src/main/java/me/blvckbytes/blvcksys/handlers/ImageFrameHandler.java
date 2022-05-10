package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.SkinSection;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
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

  public ImageFrameHandler(
    @AutoInject ILogger logger
  ) {
    this.logger = logger;
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

    for (Player t : Bukkit.getOnlinePlayers())
      group.setFramebuffer(t, getHead(t));
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    for (ItemFrameGroup group : groups)
      group.setFramebuffer(e.getPlayer(), getHead(e.getPlayer()));
  }

  /**
   * Get the player's head as an image
   * @param p Target player
   * @return Head image or null on errors
   */
  private BufferedImage getHead(Player p) {
    try {
      URL skin = p.getPlayerProfile().getTextures().getSkin();

      if (skin == null)
        return null;

      return SkinSection.HEAD_FRONT.cut(ImageIO.read(skin));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
