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
  private ConfigValue invalidInteger;
  private ConfigValue invalidFloat;
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
  private ConfigValue durabilityNone;
  private ConfigValue durabilityChanged;
  private ConfigValue durabilityUnbreakableSet;
  private ConfigValue durabilityUnbreakableReset;
  private ConfigValue durabilityUnbreakableNone;
  private ConfigValue durabilityUnbreakableAlready;
  private ConfigValue attributesNone;
  private ConfigValue attributesReset;
  private ConfigValue attributeRemoved;
  private ConfigValue attributeAdded;
  private ConfigValue attributeAmountPrompt;
  private ConfigValue skullownerSet;
  private ConfigValue skullownerNotLoadable;
  private ConfigValue skullownerPrompt;
  private ConfigValue potioneffectExtended;
  private ConfigValue potioneffectExtendedAlready;
  private ConfigValue potioneffectUpgraded;
  private ConfigValue potioneffectUpgradedAlready;
  private ConfigValue potioneffectEnhancementRemove;
  private ConfigValue potioneffectEnhancementNone;
  private ConfigValue potioneffectEnhancementUnsupported;
  private ConfigValue potioneffectCustomRemove;
  private ConfigValue potioneffectCustomAdded;
  private ConfigValue potioneffectDurationPrompt;
  private ConfigValue potioneffectAmplifierPrompt;
  private ConfigValue potioneffectCustomNone;
  private ConfigValue potioneffectCustomReset;
  private ConfigValue colorSet;
  private ConfigValue colorReset;
  private ConfigValue colorNone;
  private ConfigValue colorPrompt;
  private ConfigValue colorInvalid;
  private ConfigValue bookTitleSet;
  private ConfigValue bookTitleNone;
  private ConfigValue bookTitleReset;
  private ConfigValue bookTitlePrompt;
  private ConfigValue bookAuthorSet;
  private ConfigValue bookAuthorNone;
  private ConfigValue bookAuthorReset;
  private ConfigValue bookAuthorPrompt;
  private ConfigValue bookGenerationSet;
  private ConfigValue bookGenerationNone;
  private ConfigValue bookGenerationReset;
  private ConfigValue bookPageSingle;
  private ConfigValue bookPageRemoved;
  private ConfigValue bookEdited;
  private ConfigValue bookEditPrompt;
  private ConfigValue customModelDataSet;
  private ConfigValue customModelDataReset;
  private ConfigValue customModelDataNone;
  private ConfigValue customModelDataPrompt;
  private ConfigValue fireworkPowerPrompt;
  private ConfigValue fireworkPowerSet;
  private ConfigValue fireworkEffectsNone;
  private ConfigValue fireworkEffectsReset;
  private ConfigValue fireworkEffectsAdded;
  private ConfigValue fireworkEffectsRemoved;
  private ConfigValue fireworkEffectsSeparator;
  private ConfigValue compassLocationSet;
  private ConfigValue compassLocationReset;
  private ConfigValue compassLocationNone;
  private ConfigValue bannerPatternsAdded;
  private ConfigValue bannerPatternsRemoved;
  private ConfigValue bannerPatternsNone;
  private ConfigValue bannerPatternsCleared;
  private ConfigValue amountChanged;

  @Override
  public Object defaultFor(Class<?> type, String field) {
    if (type == ConfigValue.class)
      return ConfigValue.immediate("&cundefined");
    return super.defaultFor(type, field);
  }
}
