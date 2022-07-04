package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.AccessLevel;
import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.sections.CSMap;
import me.blvckbytes.blvcksys.config.sections.ItemStackSection;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/01/2022

  Represents a section containing all items on the IE choices screens.
*/
@Getter
public class IEItemsChoicesSection extends AConfigSection {

  private ItemStackSection material;
  private ItemStackSection enchantmentActive;
  private ItemStackSection enchantmentInactive;
  private ItemStackSection flag;
  private ItemStackSection lore;
  private ItemStackSection attributeNew;
  private ItemStackSection attributeExisting;
  private ItemStackSection equipment;
  private ItemStackSection operation;
  private ItemStackSection potionType;
  private ItemStackSection potionEffect;
  private ItemStackSection color;
  private ItemStackSection generation;
  private ItemStackSection page;
  private ItemStackSection fireworkType;
  private ItemStackSection fireworkEffect;
  private ItemStackSection patternNew;
  private ItemStackSection patternExisting;
  private ItemStackSection bannerDyeColor;
  private ItemStackBuilder flickerYes;
  private ItemStackBuilder flickerNo;
  private ItemStackBuilder trailYes;
  private ItemStackBuilder trailNo;

  @Getter(AccessLevel.PRIVATE)
  @CSMap(k = Color.class, v = Material.class)
  private Map<Color, Material> colorMaterials;

  @Getter(AccessLevel.PRIVATE)
  @CSMap(k = Enchantment.class, v = Material.class)
  private Map<Enchantment, Material> enchantmentMaterials;

  @Override
  public Object defaultFor(Class<?> type, String field) {
    if (type == ItemStackBuilder.class)
      return new ItemStackBuilder(Material.BARRIER).withName(ConfigValue.immediate("&cundefined"));
    return super.defaultFor(type, field);
  }

  public Material lookupColorMaterial(Color color) {
    if (colorMaterials == null)
      return Material.BARRIER;
    return colorMaterials.getOrDefault(color, Material.BARRIER);
  }

  public Material lookupEnchantmentMaterial(Enchantment ench) {
    if (enchantmentMaterials == null)
      return Material.BARRIER;
    return enchantmentMaterials.getOrDefault(ench, Material.BARRIER);
  }
}
