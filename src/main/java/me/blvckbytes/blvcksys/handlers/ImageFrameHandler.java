package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.SkinSection;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.*;

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

    try {

      Map<Player, BufferedImage> imgs = new HashMap<>();
      for (Player t : Bukkit.getOnlinePlayers())
        imgs.put(t, ImageIO.read(Objects.requireNonNull(t.getPlayerProfile().getTextures().getSkin())));

      SkinSection[] sections = SkinSection.values();

      Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {

        int i = 0;

        @Override
        public void run() {
          for (Player t : imgs.keySet()) {
            BufferedImage img = imgs.get(t);
            SkinSection sec = sections[i % sections.length];

            group.setFramebuffer(t, sec.cut(img));
            t.sendMessage(sec.name());

            i++;
          }
        }
      }, 0L, 40L);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
