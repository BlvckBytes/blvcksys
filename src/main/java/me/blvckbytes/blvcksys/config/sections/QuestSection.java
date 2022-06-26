package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/24/2022

  Represents a quest with it's name, a representitive item for the GUI
  as well as all of it's stages, which are to be completed sequentially.
*/
@Getter
public class QuestSection extends AConfigSection {

  // Name of the quest for messages
  private ConfigValue name;

  // Representitive item for GUI views
  private @Nullable ItemStackBuilder representitive;

  // Stages of this quest, all stages are to be completed in
  // order to complete the whole quest
  private QuestStageSection[] stages;

  public QuestSection() {
    this.stages = new QuestStageSection[0];
    this.name = ConfigValue.immediate("undefined");
  }
}
