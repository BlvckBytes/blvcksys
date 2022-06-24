package me.blvckbytes.blvcksys.config.sections;

import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/24/2022

  Represents a quest with it's name, a representitive item for the GUI
  as well as all of it's stages, which are to be completed sequentially.
*/
public class QuestSection extends AConfigSection {

  // Name of the quest for messages
  private @Nullable ConfigValue name;

  // Representitive item for GUI views
  private @Nullable ItemStack representitive;

  // Stages of this quest, all stages are to be completed in
  // order to complete the whole quest
  private @Nullable QuestStageSection[] stages;

}
