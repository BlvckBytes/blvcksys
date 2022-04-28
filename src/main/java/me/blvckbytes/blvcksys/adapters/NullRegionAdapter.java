package me.blvckbytes.blvcksys.adapters;

import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Implements the region interaction interface as a dummy, only used for
  development so that WorldGuard doesn't have to be loaded and re-loaded
  all the time, as it's quite heavy.
*/
@AutoConstruct
public class NullRegionAdapter implements IRegionAdapter {

  @Override
  public boolean canBuild(Player p, Location loc) {
    return true;
  }
}
