package me.blvckbytes.blvcksys.handlers.gui;

import com.mojang.authlib.GameProfile;
import me.blvckbytes.blvcksys.config.ConfigValue;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
    if (profile != null) {
      try {
        Field profileField = meta.getClass().getDeclaredField("profile");
        profileField.setAccessible(true);
        profileField.set(meta, profile);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
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
   * Hides all attributes on this item
   */
  public ItemStackBuilder hideAttributes() {
    if (this.meta != null)
      this.meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    return this;
  }

  /**
   * Add a color to this item (only applicable to leather armor)
   * @param color Color to add
   */
  public ItemStackBuilder withColor(Color color) {
    if (this.meta != null && this.meta instanceof LeatherArmorMeta lam)
      lam.setColor(color);
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
   */
  public ItemStackBuilder withLore(ConfigValue lore) {
    if (this.meta != null) {
      List<String> lines = meta.getLore() == null ? new ArrayList<>() : meta.getLore();
      lines.addAll(lore.asList());
      meta.setLore(lines);
    }
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
}
