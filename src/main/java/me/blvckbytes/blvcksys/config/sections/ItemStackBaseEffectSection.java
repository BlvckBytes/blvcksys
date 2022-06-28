package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Represents the base effect set on a potion item stack.
*/
@Getter
public class ItemStackBaseEffectSection extends AConfigSection {

  private @Nullable PotionType type;
  private @Nullable Boolean extended;
  private @Nullable Boolean upgraded;

  /**
   * Convert the properties of this section to a PotionData object
   */
  public PotionData asData() {
    boolean _upgraded = upgraded != null && upgraded;
    boolean _extended = extended != null && extended;

    // Potions cannot be both extended and upgraded at the same
    // time, focus the priority on the upgraded flag
    return new PotionData(
      type == null ? PotionType.AWKWARD : type,
      !_upgraded && _extended, _upgraded
    );
  }

  /**
   * Compares all available values of this section against the
   * provided data and checks if they match
   * @param data Target data
   */
  public boolean describesData(PotionData data) {
    if (type != null && type != data.getType())
      return false;

    if (extended != null && extended != data.isExtended())
      return false;

    if (upgraded != null && upgraded != data.isUpgraded())
      return false;

    // All checks passed
    return true;
  }
}
