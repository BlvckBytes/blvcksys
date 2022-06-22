package me.blvckbytes.blvcksys.handlers.gui;

import com.mojang.authlib.GameProfile;
import me.blvckbytes.blvcksys.config.ConfigValue;
import net.minecraft.util.Tuple;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Build dynamic items by making use of the config value templating system.
*/
public class ItemStackBuilder {

  private final ItemStack stack;
  private final ItemMeta meta;

  /**
   * Create a new builder for a specific material
   * @param mat Material to target
   * @param amount Amount of items
   */
  public ItemStackBuilder(Material mat, int amount) {
    this.stack = new ItemStack(mat, amount);
    this.meta = stack.getItemMeta();
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
   * Add a color to this item (only applicable to leather armor)
   * @param color Color to add
   */
  public ItemStackBuilder withColor(Supplier<Color> color, boolean condition) {
    if (condition && this.meta != null && this.meta instanceof LeatherArmorMeta lam)
      lam.setColor(color.get());
    return this;
  }

  /**
   * Set a display name
   * @param name Name to set
   */
  public ItemStackBuilder withName(ConfigValue name) {
    return withName(name, true);
  }

  /**
   * Set a display name conditionally
   * @param name Name to set
   * @param condition Boolean which has to evaluate to true in order to apply the name
   */
  public ItemStackBuilder withName(ConfigValue name, boolean condition) {
    if (condition && this.meta != null)
      this.meta.setDisplayName(name.asScalar());
    return this;
  }

  /**
   * Add a lore to the existing lore
   * @param lore Lines to set
   * @param condition Boolean which has to evaluate to true in order to apply the lore
   */
  public ItemStackBuilder withLore(ConfigValue lore, boolean condition) {
    if (condition && this.meta != null) {
      List<String> lines = meta.getLore() == null ? new ArrayList<>() : meta.getLore();
      lines.addAll(lore.asList());
      meta.setLore(lines);
    }
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
   * Build this item by applying the cached meta to the stack
   */
  public ItemStack build() {
    if (meta != null)
      stack.setItemMeta(meta);
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
}
