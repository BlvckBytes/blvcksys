package me.blvckbytes.blvcksys.handlers.gui;

import com.mojang.authlib.GameProfile;
import me.blvckbytes.blvcksys.config.ConfigValue;
import net.minecraft.util.Tuple;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Build dynamic items by making use of the config value templating system.
*/
public class ItemStackBuilder {

  private final Material mat;
  private final int amount;
  private ConfigValue displayName;
  private ConfigValue lore;
  private GameProfile profile;

  /**
   * Create a new builder for a player-head
   * @param profile Profile to apply to the head
   * @param amount Amount of items
   */
  public ItemStackBuilder(GameProfile profile, int amount) {
    this.mat = Material.PLAYER_HEAD;
    this.amount = amount;
    this.profile = profile;
  }

  /**
   * Create a new builder based on an existing item stack
   * @param from Existing item stack to mimic
   * @param amount Amount of items
   */
  public ItemStackBuilder(@Nullable ItemStack from, int amount) {
    if (from == null)
      this.mat = Material.BARRIER;
    else
      this.mat = from.getType();

    this.amount = amount;
  }

  /**
   * Create a new builder for a specific material
   * @param mat Material to target
   * @param amount Amount of items
   */
  public ItemStackBuilder(Material mat, int amount) {
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
  public ItemStack build(Map<String, Tuple<Pattern, String>> variables) {
    ItemStack stack = new ItemStack(this.mat, this.amount);
    ItemMeta meta = stack.getItemMeta();

    if (meta == null)
      return stack;

    meta.setDisplayName(
      displayName == null ?
        " " :
        displayName
          .withVariables(variables)
          .asScalar()
    );

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

    stack.setItemMeta(meta);
    return stack;
  }
}
