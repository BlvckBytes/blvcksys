package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Represents an enchantment applied to an item stack.
*/
@Getter
public class ItemStackEnchantmentSection extends AConfigSection {

  private @Nullable ConfigValue enchantment;
  private @Nullable ConfigValue level;

}
