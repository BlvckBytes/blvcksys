package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/29/2022

  Provides parameters to the EXP action.
*/
@Getter
public class QuestExpParameterSecton extends AConfigSection {

  private int maxLevel;

  public QuestExpParameterSecton() {
    this.maxLevel = Integer.MAX_VALUE;
  }
}
