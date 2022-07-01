package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/01/2022

  Represents a section containing all the inventory titles.
*/
@Getter
public class IETitlesSection extends AConfigSection {

  private ConfigValue home;
  private ConfigValue materialChoice;
  private ConfigValue flagChoice;
  private ConfigValue enchantmentChoice;
  private ConfigValue loreChoice;
  private ConfigValue attributeChoice;
  private ConfigValue equipmentChoice;
  private ConfigValue operationChoice;
  private ConfigValue potionTypeChoice;
  private ConfigValue potionEffectChoice;
  private ConfigValue colorChoice;
  private ConfigValue generationChoice;
  private ConfigValue pageChoice;
  private ConfigValue fireworkTypeChoice;
  private ConfigValue fireworkEffectColorChoice;
  private ConfigValue fireworkFadeColorChoice;
  private ConfigValue fireworkFlickerYesNo;
  private ConfigValue fireworkTrailYesNo;
  private ConfigValue fireworkEffectChoice;
  private ConfigValue bannerPatternChoice;
  private ConfigValue bannerPatternTypeChoice;
  private ConfigValue bannerDyeColorChoice;

  @Override
  public Object defaultFor(Class<?> type, String field) {
    if (type == ConfigValue.class)
      return ConfigValue.immediate("&cundefined");
    return super.defaultFor(type, field);
  }
}
