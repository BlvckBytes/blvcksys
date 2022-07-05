package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.sections.CSAlways;
import me.blvckbytes.blvcksys.config.sections.GuiLayoutSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/05/2022

  Represents a section containing all GUI layouts.
*/
@Getter
@CSAlways
public class IELayoutsSection extends AConfigSection {

  private GuiLayoutSection home;
  private GuiLayoutSection singleChoice;
  private GuiLayoutSection multipleChoice;
  private GuiLayoutSection yesNo;

}
