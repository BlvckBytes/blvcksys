package me.blvckbytes.blvcksys.handlers.gui;

import com.mojang.authlib.GameProfile;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.config.ConfigValue;
import net.minecraft.util.Tuple;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Build dynamic items by making use of the config value templating system.
*/
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemStackBuilder {

  private final ItemStack stack;
  private final ItemMeta meta;

  // Name and lore should only be evaluated at the build-step
  // in order to allow for extra variable injection
  private @Nullable ConfigValue name;
  private final List<ConfigValue> lore;

  /**
   * Create a new builder for a specific material
   * @param mat Material to target
   * @param amount Amount of items
   */
  public ItemStackBuilder(Material mat, int amount) {
    this.stack = new ItemStack(mat, amount);
    this.meta = stack.getItemMeta();
    this.lore = new ArrayList<>();
  }

  /**
   * Create a new builder for a player-head
   * @param profile Profile to apply to the head
   */
  public ItemStackBuilder(GameProfile profile) {
    this(Material.PLAYER_HEAD, 1);

    // Is a player-head where textures should be applied
    // and there has been a profile provided
    if (profile != null)
      setHeadProfile(profile);
  }

  /**
   * Create a new builder for a specific material
   * @param mat Material to target
   */
  public ItemStackBuilder(Material mat) {
    this(mat, 1);
  }

  /**
   * Create a new builder based on an existing item stack
   * @param from Existing item stack to mimic
   * @param amount Amount of items
   */
  public ItemStackBuilder(ItemStack from, int amount) {
    this.stack = from.clone();
    this.stack.setAmount(amount);
    this.meta = stack.getItemMeta();
    this.lore = new ArrayList<>();
  }

  /**
   * Add an enchantment with a specific level to this item
   * @param enchantment Enchantment to add
   * @param level Level to add the enchantment with
   */
  public ItemStackBuilder withEnchantment(Enchantment enchantment, int level) {
    if (this.meta != null)
      this.meta.addEnchant(enchantment, level, true);
    return this;
  }

  /**
   * Add enchantments to the item
   * @param enchantments Enchantments to add
   * @param condition Boolean which has to evaluate to true in order to apply the enchantments
   */
  public ItemStackBuilder withEnchantments(Supplier<List<Tuple<Enchantment, Integer>>> enchantments, boolean condition) {
    if (condition && this.meta != null) {
      enchantments.get().forEach(ench -> withEnchantment(ench.a(), ench.b()));
    }
    return this;
  }

  /**
   * Hides all attributes on this item
   */
  public ItemStackBuilder hideAttributes() {
    if (this.meta != null)
      this.meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    return this;
  }

  /**
   * Add a list of itemflags to this item
   */
  public ItemStackBuilder withFlags(Supplier<List<ItemFlag>> flags, boolean condition) {
    if (condition && this.meta != null)
      flags.get().forEach(this.meta::addItemFlags);
    return this;
  }

  /**
   * Add a color to this item (only applicable to leather armor or potions)
   * @param color Color to add
   */
  public ItemStackBuilder withColor(Supplier<Color> color, boolean condition) {
    if (!condition)
      return this;

    if (this.meta instanceof LeatherArmorMeta lam)
      lam.setColor(color.get());

    else if (this.meta instanceof PotionMeta pm)
      pm.setColor(color.get());

    else if (this.meta instanceof MapMeta mm)
      mm.setColor(color.get());

    return this;
  }

  /**
   * Set the base effect of this item (only applicable to potions)
   * @param data Base effect to set
   * @param condition Boolean which has to evaluate to true in order to apply the effect
   */
  public ItemStackBuilder withBaseEffect(Supplier<PotionData> data, boolean condition) {
    if (!condition)
      return this;

    if (this.meta instanceof PotionMeta pm)
      pm.setBasePotionData(data.get());

    return this;
  }

  /**
   * Add custom effects to this item (only applicable to potions)
   * @param effects Custom effects to add
   * @param condition Boolean which has to evaluate to true in order to apply the effects
   */
  public ItemStackBuilder withCustomEffects(Supplier<List<PotionEffect>> effects, boolean condition) {
    if (!condition)
      return this;

    if (this.meta instanceof PotionMeta pm)
      effects.get().forEach(effect -> pm.addCustomEffect(effect, true));

    return this;
  }

  /**
   * Set a display name
   * @param name Name to set
   */
  public ItemStackBuilder withName(ConfigValue name) {
    return this.withName(name, true);
  }

  /**
   * Set a display name conditionally
   * @param name Name to set
   * @param condition Boolean which has to evaluate to true in order to apply the name
   */
  public ItemStackBuilder withName(ConfigValue name, boolean condition) {
    if (condition)
      this.name = name;
    return this;
  }

  /**
   * Set a display name conditionally
   * @param name Name to set
   * @param condition Boolean which has to evaluate to true in order to apply the name
   */
  public ItemStackBuilder withName(Supplier<ConfigValue> name, boolean condition) {
    if (condition)
      this.name = name.get();
    return this;
  }

  /**
   * Add a lore to the existing lore conditionally
   * @param lore Lines to set
   * @param condition Boolean which has to evaluate to true in order to apply the lore
   */
  public ItemStackBuilder withLore(ConfigValue lore, boolean condition) {
    if (condition)
      this.lore.add(lore);
    return this;
  }

  /**
   * Add a lore to the existing lore conditionally
   * @param lore Lines to set
   * @param condition Boolean which has to evaluate to true in order to apply the lore
   */
  public ItemStackBuilder withLore(Supplier<ConfigValue> lore, boolean condition) {
    if (condition)
      this.lore.add(lore.get());
    return this;
  }

  /**
   * Add a lore to the existing lore
   * @param lore Lines to set
   */
  public ItemStackBuilder withLore(ConfigValue lore) {
    return withLore(lore, true);
  }

  /**
   * Set the head owner's profile of this item
   * @param profile Profile to set
   * @param condition Boolean which has to evaluate to true in order to apply the profile
   */
  public ItemStackBuilder withProfile(Supplier<GameProfile> profile, boolean condition) {
    if (condition && this.meta != null)
      setHeadProfile(profile.get());
    return this;
  }

  /**
   * Build this item by applying the cached metainfo to the stack
   */
  public ItemStack build() {
    return this.build(null);
  }

  /**
   * Build this item by applying the cached metainfo to the stack
   * @param variables Optional variables to apply to the name and lore
   */
  public ItemStack build(@Nullable Map<String, String> variables) {
    // There is a meta to manipulate
    if (meta != null) {

      // Clone the item meta to leave the state of this builder untouched
      ItemMeta buildMeta = meta.clone();

      // Name requested
      if (name != null) {
        if (variables != null)
          name.withVariables(variables);
        buildMeta.setDisplayName(name.asScalar());
      }

      // Lore requested
      if (lore.size() > 0) {
        List<String> lines = buildMeta.getLore() == null ? new ArrayList<>() : buildMeta.getLore();
        lore.forEach(line -> lines.addAll(
          line
            .withVariables(variables)
            .asList()
        ));
        buildMeta.setLore(lines);
      }

      stack.setItemMeta(buildMeta);
    }

    return stack;
  }

  /**
   * Sets the head game profile of the item-meta
   * @param profile Game profile to set
   */
  private void setHeadProfile(GameProfile profile) {
    if (this.meta == null || !(this.meta instanceof SkullMeta))
      return;

    try {
      Field profileField = meta.getClass().getDeclaredField("profile");
      profileField.setAccessible(true);
      profileField.set(meta, profile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create a carbon copy of this instance
   */
  public ItemStackBuilder copy() {
    return new ItemStackBuilder(
      stack.clone(), meta.clone(), name, new ArrayList<>(lore)
    );
  }
}
