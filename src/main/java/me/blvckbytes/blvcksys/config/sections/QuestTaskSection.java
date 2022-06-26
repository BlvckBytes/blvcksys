package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/24/2022

  Represents one task of a quest, which listens for a certain action
  of the player for a certain number of times and allows for granular
  selection by meta-information using the dedicated parameters model.
*/
@Getter
public class QuestTaskSection extends AConfigSection {

  private @Nullable QuestAction action;
  private Integer count;
  private @Nullable Object parameters;

  public QuestTaskSection() {
    this.count = 1;
  }

  @Override
  public Class<?> runtimeDecide(String field) {
    // Decide the parameter type based on the selected action
    if (field.equals("parameters") && action != null)
      return action.getParameterType();
    return super.runtimeDecide(field);
  }
}
