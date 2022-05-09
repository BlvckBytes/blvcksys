package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.awt.*;
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
      group.setFramebuffer(t, genFrameBuf());
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    for (ItemFrameGroup group : groups)
      group.setFramebuffer(e.getPlayer(), genFrameBuf());
  }

  // Just displays green with red lines at the edges of individual frames (tile pattern)
  private Color[][] genFrameBuf() {
    int width = 4, height = 3;
    int pWidth = width * 128, pHeight = 128 * height;

    Color[][] fbuf = new Color[pWidth][pHeight];

    for (int x = 0; x < pWidth; x++) {
      for (int y = 0; y < pHeight; y++) {
        boolean isEdge = x % 128 == 0 || y % 128 == 0;
        fbuf[x][y] = new Color(isEdge ? 255 : 0, isEdge ? 0 : 255, 0);
      }
    }

    return fbuf;
  }
}
