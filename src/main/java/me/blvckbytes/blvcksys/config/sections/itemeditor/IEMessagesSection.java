package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/01/2022

  Represents a section containing all chat messages.
*/
@Getter
public class IEMessagesSection extends AConfigSection {

  private ConfigValue metaUnavailable;
  private ConfigValue missingPermission;
  private ConfigValue materialChanged;
  private ConfigValue flagChanged;
  private ConfigValue enchantmentAdded;
  private ConfigValue enchantmentRemoved;
  private ConfigValue enchantmentLevelPrompt;
  private ConfigValue displaynameSet;
  private ConfigValue displaynameReset;
  private ConfigValue displaynameNone;
  private ConfigValue displaynamePrompt;
  private ConfigValue loreReset;
  private ConfigValue loreLineRemoved;
  private ConfigValue loreNone;
  private ConfigValue loreLinePrompt;
  private ConfigValue loreAdded;

  @Override
  public Object defaultFor(Class<?> type, String field) {
    if (type == ConfigValue.class)
      return ConfigValue.immediate("&cundefined");
    return super.defaultFor(type, field);
  }
}
