package me.blvckbytes.blvcksys.handlers;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.packets.communicators.armorstand.ArmorStandProperties;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/03/2022

  Specifies all available equipment slots an armor stand has to offer.
*/
@AllArgsConstructor
public enum EquipmentSlot {
  HELMET(ArmorStandProperties::getHelmet, ArmorStandProperties::setHelmet),
  CHESTPLATE(ArmorStandProperties::getChestplate, ArmorStandProperties::setChestplate),
  LEGGINGS(ArmorStandProperties::getLeggings, ArmorStandProperties::setLeggings),
  BOOTS(ArmorStandProperties::getBoots, ArmorStandProperties::setBoots),
  MAIN_HAND(ArmorStandProperties::getHand, ArmorStandProperties::setHand),
  OFF_HAND(ArmorStandProperties::getOffHand, ArmorStandProperties::setOffHand)
  ;

  private final Function<ArmorStandProperties, ItemStack> getter;
  private final BiConsumer<ArmorStandProperties, ItemStack> setter;

  /**
   * Sets the property within a property wrapper
   * @param props Property wrapper
   * @param item Value to set
   */
  public void set(ArmorStandProperties props, ItemStack item) {
    setter.accept(props, item);
  }

  /**
   * Gets the property from a property wrapper
   * @param props Property wrapper
   * @return Current value
   */
  public ItemStack get(ArmorStandProperties props) {
    return getter.apply(props);
  }
}
