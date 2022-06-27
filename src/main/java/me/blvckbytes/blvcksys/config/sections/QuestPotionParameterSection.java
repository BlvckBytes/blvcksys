package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/24/2022

  Provides parameters to the quest action named BREWING.
*/
@Getter
public class QuestPotionParameterSection extends AConfigSection {

  // List of effects that the potion needs to have
  private QuestPotionParameterEffectSection[] effects;

  // Type of the potion
  private Boolean splash, lingering;

  // If true, any of the effects may match, if false, all have to be present
  private Boolean anyOf;

  public QuestPotionParameterSection() {
    this.effects = new QuestPotionParameterEffectSection[0];
    this.anyOf = true;
    this.splash = false;
    this.lingering = false;
  }
}
