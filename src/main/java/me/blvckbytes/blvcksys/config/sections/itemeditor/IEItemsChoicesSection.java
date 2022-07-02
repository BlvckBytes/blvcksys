package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.sections.ItemStackSection;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import org.bukkit.Material;

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

  @Override
  public Object defaultFor(Class<?> type, String field) {
    if (type == ItemStackBuilder.class)
      return new ItemStackBuilder(Material.BARRIER).withName(ConfigValue.immediate("&cundefined"));
    return super.defaultFor(type, field);
  }
}
