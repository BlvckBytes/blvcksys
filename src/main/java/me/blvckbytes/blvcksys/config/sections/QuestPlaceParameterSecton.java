package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/29/2022

  Provides parameters to the PLACE action.
*/
@Getter
public class QuestPlaceParameterSecton extends AConfigSection {

  // Item held in the main hand by the player while placing the block
  private @Nullable ItemStackSection hand;

  // Target block to be placed, any of
  private BlockSection[] blocks;

  public QuestPlaceParameterSecton() {
    this.blocks = new BlockSection[0];
  }
}
