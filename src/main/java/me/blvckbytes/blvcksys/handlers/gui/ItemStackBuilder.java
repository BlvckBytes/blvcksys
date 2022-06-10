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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Build dynamic items by making use of the config value templating system.
*/
public class ItemStackBuilder {

  private final List<ItemFlag> flags;
  private final Map<Enchantment, Integer> enchantments;
  private final Material mat;
  private final int amount;
  private ConfigValue displayName;
  private ConfigValue lore;
  private GameProfile profile;
  private ItemStack stack;
  private ItemMeta meta;
  private Color color;

  /**
   * Create a new builder for a player-head
   * @param profile Profile to apply to the head
   */
  public ItemStackBuilder(GameProfile profile) {
    this.mat = Material.PLAYER_HEAD;
    this.amount = 1;
    this.profile = profile;
    this.enchantments = new HashMap<>();
    this.flags = new ArrayList<>();
  }

  /**
   * Create a new builder based on an existing item stack
   * @param from Existing item stack to mimic
   * @param amount Amount of items
   */
  public ItemStackBuilder(@Nullable ItemStack from, int amount) {
    if (from == null)
      this.mat = Material.BARRIER;
    else {
      this.mat = from.getType();
      this.stack = from.clone();
      this.meta = stack.getItemMeta();
    }

    this.flags = new ArrayList<>();
    this.enchantments = new HashMap<>();
    this.amount = amount;
  }

  /**
   * Create a new builder for a specific material
   * @param mat Material to target
   * @param amount Amount of items
   */
  public ItemStackBuilder(Material mat, int amount) {
    this.enchantments = new HashMap<>();
    this.flags = new ArrayList<>();
    this.mat = mat;
    this.amount = amount;
  }

  /**
   * Create a new builder for a specific material
   * @param mat Material to target
   */
  public ItemStackBuilder(Material mat) {
    this(mat, 1);
  }

  /**
   * Add an enchantment with a specific level to this item
   * @param enchantment Enchantment to add
   * @param level Level to add the enchantment with
   */
  public ItemStackBuilder withEnchantment(Enchantment enchantment, int level) {
    this.enchantments.put(enchantment, level);
    return this;
  }

  /**
   * Hides all attributes on this item
   */
  public ItemStackBuilder hideAttributes() {
    this.flags.add(ItemFlag.HIDE_ATTRIBUTES);
    return this;
  }

  /**
   * Add a color to this item (only applicable to leather armor)
   * @param color Color to add
   */
  public ItemStackBuilder withColor(Color color) {
    this.color = color;
    return this;
  }

  /**
   * Set a display name
   * @param name Name to set
   */
  public ItemStackBuilder withName(ConfigValue name) {
    this.displayName = name;
    return this;
  }

  /**
   * Set a lore (lines of text when hovering)
   * @param lore Lines to set
   */
  public ItemStackBuilder withLore(ConfigValue lore) {
    this.lore = lore;
    return this;
  }

  /**
   * Build this item without any additional template variables
   */
  public ItemStack build() {
    return build(new HashMap<>());
  }

  /**
   * Build this item with additional template variables
   * @param variables Variables to apply
   */
  public ItemStack build(Map<String, String> variables) {
    if (stack == null)
      stack = new ItemStack(this.mat, this.amount);
    else {
      stack.setType(this.mat);
      stack.setAmount(this.amount);
    }

    if (meta == null)
      meta = stack.getItemMeta();

    if (meta == null)
      return stack;

    if (displayName != null) {
      meta.setDisplayName(
        displayName
          .withVariables(variables)
          .asScalar()
      );
    }

    meta.setLore(
      lore == null ?
        new ArrayList<>() :
        lore
          .withVariables(variables)
          .asList()
    );

    // Is a player-head where textures should be applied
    if (mat == Material.PLAYER_HEAD && profile != null) {
      try {
        Field profileField = meta.getClass().getDeclaredField("profile");
        profileField.setAccessible(true);
        profileField.set(meta, profile);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    for (ItemFlag flag : this.flags)
      meta.addItemFlags(flag);

    if (color != null && meta instanceof LeatherArmorMeta lam)
      lam.setColor(color);

    for (Map.Entry<Enchantment, Integer> ench : enchantments.entrySet()) {
      meta.addEnchant(ench.getKey(), ench.getValue(), true);
    }

    stack.setItemMeta(meta);
    return stack;
  }
}
