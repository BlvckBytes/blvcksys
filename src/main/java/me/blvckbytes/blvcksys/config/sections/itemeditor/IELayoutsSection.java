package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.sections.CSAlways;
import me.blvckbytes.blvcksys.config.sections.GuiLayoutSection;

@Getter
@CSAlways
public class IELayoutsSection extends AConfigSection {

  private GuiLayoutSection home;

}
