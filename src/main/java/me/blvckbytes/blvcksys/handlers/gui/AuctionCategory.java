package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/09/2022

  Lists all available auction categories as well as their representitive
  icons and displaynames.
*/
@AllArgsConstructor
public enum AuctionCategory {
  COMBAT(
    Material.DIAMOND_SWORD, ConfigKey.GUI_AH_CAT_COMBAT_NAME,
    mat -> (
      mat == Material.DIAMOND_SWORD ||
      mat == Material.NETHERITE_SWORD ||
      mat == Material.IRON_SWORD ||
      mat == Material.GOLDEN_SWORD ||
      mat == Material.STONE_SWORD ||
      mat == Material.WOODEN_SWORD ||
      mat == Material.BOW ||
      mat == Material.CROSSBOW ||
      mat == Material.ARROW ||
      mat == Material.SPECTRAL_ARROW ||
      mat == Material.TIPPED_ARROW ||
      mat == Material.TRIDENT ||
      mat == Material.SHIELD ||
      mat == Material.TOTEM_OF_UNDYING ||
      mat == Material.GOLDEN_APPLE ||
      mat == Material.ENCHANTED_GOLDEN_APPLE ||
      mat == Material.ENDER_PEARL
    )
  ),

  ARMOR(
    Material.IRON_CHESTPLATE, ConfigKey.GUI_AH_CAT_ARMOR_NAME,
    mat -> (
      mat == Material.LEATHER_HELMET ||
      mat == Material.LEATHER_CHESTPLATE ||
      mat == Material.LEATHER_LEGGINGS ||
      mat == Material.LEATHER_BOOTS ||
      mat == Material.GOLDEN_HELMET ||
      mat == Material.GOLDEN_CHESTPLATE ||
      mat == Material.GOLDEN_LEGGINGS ||
      mat == Material.GOLDEN_BOOTS ||
      mat == Material.CHAINMAIL_HELMET ||
      mat == Material.CHAINMAIL_CHESTPLATE ||
      mat == Material.CHAINMAIL_LEGGINGS ||
      mat == Material.CHAINMAIL_BOOTS ||
      mat == Material.IRON_HELMET ||
      mat == Material.IRON_CHESTPLATE ||
      mat == Material.IRON_LEGGINGS ||
      mat == Material.IRON_BOOTS ||
      mat == Material.DIAMOND_HELMET ||
      mat == Material.DIAMOND_CHESTPLATE ||
      mat == Material.DIAMOND_LEGGINGS ||
      mat == Material.DIAMOND_BOOTS ||
      mat == Material.NETHERITE_HELMET ||
      mat == Material.NETHERITE_CHESTPLATE ||
      mat == Material.NETHERITE_LEGGINGS ||
      mat == Material.NETHERITE_BOOTS ||
      mat == Material.TURTLE_HELMET
    )
  ),

  TOOLS(
    Material.GOLDEN_AXE, ConfigKey.GUI_AH_CAT_TOOLS_NAME,
    mat -> (
      mat == Material.WOODEN_SHOVEL ||
      mat == Material.WOODEN_PICKAXE ||
      mat == Material.WOODEN_AXE ||
      mat == Material.WOODEN_HOE ||
      mat == Material.STONE_SHOVEL ||
      mat == Material.STONE_PICKAXE ||
      mat == Material.STONE_AXE ||
      mat == Material.STONE_HOE ||
      mat == Material.GOLDEN_SHOVEL ||
      mat == Material.GOLDEN_PICKAXE ||
      mat == Material.GOLDEN_AXE ||
      mat == Material.GOLDEN_HOE ||
      mat == Material.IRON_SHOVEL ||
      mat == Material.IRON_PICKAXE ||
      mat == Material.IRON_AXE ||
      mat == Material.IRON_HOE ||
      mat == Material.DIAMOND_SHOVEL ||
      mat == Material.DIAMOND_PICKAXE ||
      mat == Material.DIAMOND_AXE ||
      mat == Material.DIAMOND_HOE ||
      mat == Material.NETHERITE_SHOVEL ||
      mat == Material.NETHERITE_PICKAXE ||
      mat == Material.NETHERITE_AXE ||
      mat == Material.NETHERITE_HOE ||
      mat == Material.FISHING_ROD ||
      mat == Material.SHEARS
    )
  ),

  BUILDING(
    Material.OAK_LOG, ConfigKey.GUI_AH_CAT_BUILDING_NAME,
    Material::isBlock
  ),

  MISC(
    Material.DEAD_BUSH, ConfigKey.GUI_AH_CAT_MISC_NAME,
    null // Miscellaneous just takes on all remaining materials
  ),

  ALL(
    Material.CHEST_MINECART, ConfigKey.GUI_AH_CAT_ALL_NAME,
    null // This isn't a category in that sense
  )
  ;

  @Getter
  private final Material mat;
  private final ConfigKey name;
  private final @Nullable Function<Material, Boolean> member;

  /**
   * Get the display name of this catecory
   * @param cfg Config ref
   * @return Config value containing the name
   */
  public ConfigValue getName(IConfig cfg) {
    return cfg.get(name);
  }

  /**
   * Automatically determine an item's category by it's type
   * @param item Item to detect the category of
   * @return Category of the item
   */
  public static AuctionCategory fromItem(ItemStack item) {
    // Scan through all categories
    for (AuctionCategory cat : AuctionCategory.values()) {
      if (cat.member != null && cat.member.apply(item.getType()))
        return cat;
    }

    // If no category could be determined, it's misc
    return MISC;
  }
}

