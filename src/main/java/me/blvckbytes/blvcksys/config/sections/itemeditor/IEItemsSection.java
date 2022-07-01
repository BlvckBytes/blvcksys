package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.sections.CSAlways;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/01/2022

  Represents a section containing all groups of GUI items.
*/
@Getter
public class IEItemsSection extends AConfigSection {

  @CSAlways
  private IEItemsGenericSection generic;

  @CSAlways
  private IEItemsHomeSection home;

  @CSAlways
  private IEItemsChoicesSection choices;

}
