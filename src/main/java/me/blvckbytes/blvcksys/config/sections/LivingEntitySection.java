package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/29/2022

  Represents the properties of a fully describeable, living entity.
*/
@Getter
public class LivingEntitySection extends AConfigSection {

  // TODO: Add more properties
  private @Nullable ConfigValue name;
  private @Nullable EntityType type;

  /**
   * Compares all available values of this section against the
   * provided entity and checks if they match
   * @param e Target entity
   */
  public boolean describesEntity(LivingEntity e) {
    // Name mismatch
    if (name != null && !name.asScalar().equals(e.getCustomName()))
      return false;

    // Type mismatch
    if (type != null && !type.equals(e.getType()))
      return false;

    // Passed all checks
    return true;
  }
}
