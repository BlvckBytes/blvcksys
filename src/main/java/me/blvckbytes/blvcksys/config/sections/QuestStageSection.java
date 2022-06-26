package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/24/2022

  Represents a stage of a quest, a level so to speak, which may have
  multiple tasks that need to be completed to complete that stage. If
  necessary, tasks may only be completed in order.
*/
@Getter
public class QuestStageSection extends AConfigSection {

  // Name of the stage for messages
  private @Nullable ConfigValue name;

  // Representitive item for GUI views
  private @Nullable ItemStackBuilder representitive;

  // List of tasks which need to be completed in order to complete this stage
  private QuestTaskSection[] tasks;

  // Whether the tasks need to be completed in the specified sequence
  private boolean tasksInOrder;

  public QuestStageSection() {
    this.tasks = new QuestTaskSection[0];
    this.tasksInOrder = true;
  }
}
