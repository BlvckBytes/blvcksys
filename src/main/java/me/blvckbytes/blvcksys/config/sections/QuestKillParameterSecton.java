package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/29/2022

  Provides parameters to the KILL action.
*/
@Getter
public class QuestKillParameterSecton extends AConfigSection {

  // Item held in the main hand by the player while killing the entity
  private @Nullable ItemStackSection hand;

  // Target entity to be killed, any of
  private LivingEntitySection[] entities;

  // Location of the killed entity, any of
  private LocationSection[] locations;

  public QuestKillParameterSecton() {
    this.entities = new LivingEntitySection[0];
    this.locations = new LocationSection[0];
  }
}
