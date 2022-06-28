package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Represents the properties of a describeable block within the world.
*/
@Getter
public class BlockSection extends AConfigSection {

  private @Nullable Material type;
  private @Nullable Biome biome;
  private @Nullable LocationSection location;

  /**
   * Compares all available values of this section against the
   * provided block and checks if they match
   * @param b Target block
   */
  public boolean describesBlock(Block b) {
    // Block type mismatch
    if (type != null && type != b.getType())
      return false;

    // Block biome mismatch
    if (biome != null && biome != b.getBiome())
      return false;

    // Location mismatch
    if (location != null && !location.describesLocation(b.getLocation()))
      return false;

    // All checks passed
    return true;
  }
}
