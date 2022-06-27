package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Represents the base effect set on a potion item stack.
*/
@Getter
public class ItemStackBaseEffectSection extends AConfigSection {

  private PotionType type;
  private Boolean extended;
  private Boolean upgraded;

  public ItemStackBaseEffectSection() {
    this.type = PotionType.AWKWARD;
    this.extended = false;
    this.upgraded = false;
  }

  /**
   * Convert the properties of this section to a PotionData object
   */
  public PotionData asData() {
    // Potions cannot be both extended and upgraded at the same
    // time, focus the priority on the upgraded flag
    return new PotionData(type, !upgraded && extended, upgraded);
  }
}
