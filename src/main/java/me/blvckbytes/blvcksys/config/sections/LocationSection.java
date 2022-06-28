package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Represents the properties of a fully describeable location within the world.
*/
@Getter
public class LocationSection extends AConfigSection {

  private @Nullable Double x, y, z;
  private @Nullable String world;

  /**
   * Compares all available values of this section against the
   * provided location and checks if they match
   * @param loc Target location
   */
  public boolean describesLocation(Location loc) {
    // World name mismatch or no world available
    if (world != null && (loc.getWorld() == null || !world.equalsIgnoreCase(loc.getWorld().getName())))
      return false;

    // X mismatch
    if (x != null && x != loc.getX())
      return false;

    // Y mismatch
    if (y != null && y != loc.getY())
      return false;

    // Z mismatch
    if (z != null && z != loc.getZ())
      return false;

    // All checks passed
    return true;
  }
}
