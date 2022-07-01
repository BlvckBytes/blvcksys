package me.blvckbytes.blvcksys.config.sections.itemeditor;

import org.bukkit.entity.Player;
import java.util.Arrays;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/01/2022

  Supplies all permissions which exist for the item editor.
*/
public enum IEPerm {
  INCREASE("ie.increase"),
  DECREASE("ie.decrease"),
  CUSTOMMODELDATA("ie.custommodeldata"),
  MATERIAL("ie.material"),
  FLAGS("ie.flags"),
  ENCHANTMENTS("ie.flags"),
  DISPLAYNAME("ie.displayname"),
  LORE("ie.lore"),

  DURABILITY_UNBREAKABLE("ie.durability.unbreakable"),
  DURABILITY_CHANGE("ie.durability.change"),
  DURABILITY(
    DURABILITY_UNBREAKABLE.permissions,
    DURABILITY_CHANGE.permissions
  ),

  ATTRIBUTES("ie.attributes"),
  FIREWORKS("ie.fireworks"),
  COMPASS("ie.compass"),
  HEAD_OWNER("ie.headowner"),
  LEATHER_COLOR("ie.leathercolor"),
  POTION_EFFECTS("ie.potioneffects"),
  MAPS("ie.maps"),
  BOOKS("ie.books"),
  BANNERS("ie.banners"),
  ;

  private final String[] permissions;

  IEPerm(String... permissions) {
    this.permissions = permissions;
  }

  IEPerm(String[]... permissions) {
    int size = 0;
    for (String[] arr : permissions)
      size += arr.length;

    this.permissions = new String[size];

    int c = 0;
    for (String[] arr : permissions) {
      for (String permission : arr)
        this.permissions[c++] = permission;
    }
  }

  public boolean has(Player p) {
    return Arrays.stream(permissions).anyMatch(p::hasPermission);
  }

  @Override
  public String toString() {
    return permissions[0];
  }
}
