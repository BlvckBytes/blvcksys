package me.blvckbytes.blvcksys.config.sections;

import me.blvckbytes.blvcksys.config.AConfigSection;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/24/2022

  Represents one task of a quest, which listens for a certain action
  of the player for a certain number of times and allows for granular
  selection by meta-information using the dedicated parameters model.
*/
public class QuestTaskSection extends AConfigSection {

  private @Nullable QuestAction action;
  private @Nullable Integer count;
  private @Nullable Object parameters;

  @Override
  public Class<?> runtimeDecide(String field) {
    // Decide the parameter type based on the selected action
    if (field.equals("parameters") && action != null)
      return action.getParameterType();
    return super.runtimeDecide(field);
  }
}
