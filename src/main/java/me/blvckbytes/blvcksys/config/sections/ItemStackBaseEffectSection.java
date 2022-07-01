package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Represents the base effect set on a potion item stack.
*/
@Getter
public class ItemStackBaseEffectSection extends AConfigSection {

  private @Nullable ConfigValue type;
  private @Nullable Boolean extended;
  private @Nullable Boolean upgraded;

  /**
   * Convert the properties of this section to a PotionData object
   * @param variables Variables to apply while evaluating values
   */
  public PotionData asData(@Nullable Map<String, String> variables) {
    boolean _upgraded = upgraded != null && upgraded;
    boolean _extended = extended != null && extended;

    PotionType type = this.type == null ? null : this.type.withVariables(variables).asScalar(PotionType.class);

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
    PotionType type = this.type == null ? null : this.type.asScalar(PotionType.class);
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
