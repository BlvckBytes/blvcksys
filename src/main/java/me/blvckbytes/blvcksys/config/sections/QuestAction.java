package me.blvckbytes.blvcksys.config.sections;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/24/2022

  Defines all available quest actions and their matching parameter value model.
*/
@Getter
@AllArgsConstructor
public enum QuestAction {
  // Brewing potions in a brewing stand
  BREWING(QuestPotionParameterSection.class),
  // Consuming any consumable items
  CONSUME(QuestItemParameterSection.class),
  // Break blocks
  BREAK(QuestBreakParameterSecton.class)
  ;

  private final Class<? extends AConfigSection> parameterType;
}
