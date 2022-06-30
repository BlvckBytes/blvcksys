package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/30/2022

  Provides parameters to the MILK action.
*/
@Getter
public class QuestMilkParameterSecton extends AConfigSection {

  private LocationSection[] locations;

  public QuestMilkParameterSecton() {
    this.locations = new LocationSection[0];
  }
}
