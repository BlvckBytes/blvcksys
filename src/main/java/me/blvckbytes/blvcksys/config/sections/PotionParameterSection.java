package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/24/2022

  Provides parameters to the quest action named BREWING.
*/
@Getter
public class PotionParameterSection extends AConfigSection {

  // List of effects that the potion needs to have
  private @Nullable PotionParameterEffectSection[] effects;

  // If true, any of the effects may match, if false, all have to be present
  private boolean anyOf;

}
