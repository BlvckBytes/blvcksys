package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/09/2022

  Holds multiple item frame groups and handles what they display.
 */
@AutoConstruct
public class ImageFrameHandler implements IAutoConstructed {

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
    this.groups.add(new ItemFrameGroup(loc));
  }
}
