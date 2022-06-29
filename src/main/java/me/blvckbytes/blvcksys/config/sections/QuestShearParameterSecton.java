package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.bukkit.DyeColor;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/30/2022

  Provides parameters to the SHEAR action.
*/
@Getter
public class QuestShearParameterSecton extends AConfigSection {

  private DyeColor[] colors;
  private LocationSection[] locations;

  public QuestShearParameterSecton() {
    this.colors = new DyeColor[0];
    this.locations = new LocationSection[0];
  }
}
