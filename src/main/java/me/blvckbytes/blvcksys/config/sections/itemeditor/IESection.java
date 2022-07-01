package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.sections.CSAlways;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/01/2022

  Represents the root section of the config file.
*/
@Getter
public class IESection extends AConfigSection {

  @CSAlways
  private IEMessagesSection messages;

  @CSAlways
  private IETitlesSection titles;

  @CSAlways
  private IEItemsSection items;

}
