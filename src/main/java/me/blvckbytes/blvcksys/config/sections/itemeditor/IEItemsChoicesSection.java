package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.AccessLevel;
import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.sections.CSMap;
import me.blvckbytes.blvcksys.config.sections.ItemStackSection;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;

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
  private ItemStackSection flagActive;
  private ItemStackSection flagInactive;
  private ItemStackSection lore;
  private ItemStackSection attributeNew;
  private ItemStackSection attributeExisting;
  private ItemStackSection equipment;
  private ItemStackSection operation;
  private ItemStackSection potionType;
  private ItemStackSection potionEffectActive;
  private ItemStackSection potionEffectNew;
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

  @Getter(AccessLevel.PRIVATE)
  @CSMap(k = FireworkEffect.Type.class, v = Material.class)
  private Map<FireworkEffect.Type, Material> fireworkEffectTypeMaterial;

  @Getter(AccessLevel.PRIVATE)
  @CSMap(k = ItemFlag.class, v = Material.class)
  private Map<ItemFlag, Material> itemFlagMaterial;

  @Getter(AccessLevel.PRIVATE)
  @CSMap(k = Attribute.class, v = Material.class)
  private Map<Attribute, Material> attributeMaterial;

  @Getter(AccessLevel.PRIVATE)
  @CSMap(k = EquipmentSlot.class, v = Material.class)
  private Map<EquipmentSlot, Material> equipmentMaterial;

  @Getter(AccessLevel.PRIVATE)
  @CSMap(k = AttributeModifier.Operation.class, v = Material.class)
  private Map<AttributeModifier.Operation, Material> operationMaterial;

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

  public Material lookupFireworkEffectTypeMaterial(FireworkEffect.Type type) {
    if (fireworkEffectTypeMaterial == null)
      return Material.BARRIER;
    return fireworkEffectTypeMaterial.getOrDefault(type, Material.BARRIER);
  }

  public Material lookupItemFlagMaterial(ItemFlag flag) {
    if (itemFlagMaterial == null)
      return Material.BARRIER;
    return itemFlagMaterial.getOrDefault(flag, Material.BARRIER);
  }

  public Material attributeMaterial(Attribute attr) {
    if (attributeMaterial == null)
      return Material.BARRIER;
    return attributeMaterial.getOrDefault(attr, Material.BARRIER);
  }

  public Material equipmentMaterial(EquipmentSlot slot) {
    if (equipmentMaterial == null)
      return Material.BARRIER;
    return equipmentMaterial.getOrDefault(slot, Material.BARRIER);
  }

  public Material operationMaterial(AttributeModifier.Operation op) {
    if (operationMaterial == null)
      return Material.BARRIER;
    return operationMaterial.getOrDefault(op, Material.BARRIER);
  }
}
