package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.sections.CSAlways;
import me.blvckbytes.blvcksys.config.sections.ItemStackSection;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import org.bukkit.Material;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/01/2022

  Represents a section containing all items on the IE home screen.
*/
@Getter
public class IEItemsHomeSection extends AConfigSection {

  @CSAlways
  private ItemStackSection notApplicable;

  @CSAlways
  private ItemStackSection missingPermission;

  private ItemStackBuilder increase;
  private ItemStackBuilder decrease;
  private ItemStackBuilder displayMarker;
  private ItemStackBuilder customModelData;
  private ItemStackBuilder material;
  private ItemStackBuilder flags;
  private ItemStackBuilder enchantments;
  private ItemStackBuilder displayname;
  private ItemStackBuilder lore;
  private ItemStackBuilder durability;
  private ItemStackBuilder attributes;
  private ItemStackBuilder fireworks;
  private ItemStackBuilder compass;
  private ItemStackBuilder headOwner;
  private ItemStackBuilder leatherColor;
  private ItemStackBuilder potionEffects;
  private ItemStackBuilder maps;
  private ItemStackBuilder books;
  private ItemStackBuilder banners;

  @Override
  public Object defaultFor(Class<?> type, String field) {
    if (type == ItemStackBuilder.class)
      return new ItemStackBuilder(Material.BARRIER).withName(ConfigValue.immediate("&cundefined"));
    return super.defaultFor(type, field);
  }
}
