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
  BREWING(QuestPotionParameterSection.class),
  CONSUME(QuestItemParameterSection.class),
  BREAK(QuestBreakParameterSecton.class),
  PLACE(QuestPlaceParameterSecton.class),
  KILL(QuestKillParameterSecton.class),
  EXP(QuestExpParameterSecton.class),
  SHEAR(QuestShearParameterSecton.class),
  ;

  private final Class<? extends AConfigSection> parameterType;
}
