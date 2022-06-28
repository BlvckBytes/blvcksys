package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Provides parameters to the BREAK action.
*/
@Getter
public class QuestBreakParameterSecton extends AConfigSection {

  // Item held in the main hand by the player while breaking the block
  private @Nullable ItemStackSection hand;

  // Items dropped when breaking that block
  private ItemStackSection[] yield;

  // Target block to be broken
  private @Nullable BlockSection block;

  public QuestBreakParameterSecton() {
    this.yield = new ItemStackSection[0];
  }
}
