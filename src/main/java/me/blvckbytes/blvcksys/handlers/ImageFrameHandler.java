package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
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

  public ImageFrameHandler() {
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
    ItemFrameGroup group = new ItemFrameGroup(loc);
    this.groups.add(group);

    for (Player t : Bukkit.getOnlinePlayers())
      group.setFramebuffer(t, getAvatar(t));
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    for (ItemFrameGroup group : groups)
      group.setFramebuffer(e.getPlayer(), getAvatar(e.getPlayer()));
  }

  /**
   * Get the player's rendered avatar from an API
   * @param p Target player
   * @return Avatar image or null on errors
   */
  private BufferedImage getAvatar(Player p) {
    try {
      String url = "https://crafatar.com/renders/body/" + p.getUniqueId() + "?scale=10";
      return ImageIO.read(new URL(url));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
