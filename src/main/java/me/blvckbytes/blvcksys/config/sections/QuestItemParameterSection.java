package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/27/2022

  Provides parameters to any actions which need items as an input.
*/
@Getter
public class QuestItemParameterSection extends AConfigSection {

  // List of items
  private ItemStackSection[] items;

  public QuestItemParameterSection() {
    this.items = new ItemStackSection[0];
  }
}
