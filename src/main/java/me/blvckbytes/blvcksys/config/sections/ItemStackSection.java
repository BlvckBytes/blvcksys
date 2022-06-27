package me.blvckbytes.blvcksys.config.sections;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import net.minecraft.util.Tuple;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Represents the properties of a fully describeable item stack.
*/
@Getter
public class ItemStackSection extends AConfigSection {

  private @Nullable ConfigValue name;
  private int amount;
  private Material type;
  private @Nullable ConfigValue lore;
  private @Nullable ConfigValue flags;
  private @Nullable Color color;
  private ItemStackEnchantmentSection[] enchantments;
  private @Nullable GameProfile textures;
  private @Nullable ItemStackBaseEffectSection baseEffect;
  private ItemStackCustomEffectSection[] customEffects;

  // Builder cache, as instances will be reusable
  @ConfigSectionIgnore
  private ItemStackBuilder item = null;

  public ItemStackSection() {
    this.amount = 1;
    this.type = Material.BARRIER;
    this.enchantments = new ItemStackEnchantmentSection[0];
    this.customEffects = new ItemStackCustomEffectSection[0];
  }

  /**
   * Create an item stack builder from the parameters of this section
   */
  public ItemStackBuilder asItem() {
    // Cache the builder instance
    if (item == null) {
      item = new ItemStackBuilder(type, amount)
        .withName(name, name != null)
        .withLore(lore, lore != null)
        .withFlags(() -> flags.asList(ItemFlag.class), flags != null)
        .withEnchantments(() -> (
          Arrays.stream(enchantments)
            .map(es -> new Tuple<>(es.getEnchantment(), es.getLevel()))
            .toList()
        ), enchantments != null)
        .withColor(() -> color, color != null)
        .withProfile(() -> textures, textures != null)
        .withBaseEffect(() -> baseEffect.asData(), baseEffect != null)
        .withCustomEffects(() -> (
          Arrays.stream(customEffects)
            .map(effect -> effect.asEffect().orElse(null))
            .filter(Objects::nonNull)
            .toList()
        ), true);
    }

    return item;
  }
}
