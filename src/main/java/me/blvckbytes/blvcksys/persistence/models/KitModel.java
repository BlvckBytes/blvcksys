package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.inventory.Inventory;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  A kit that corresponds a unique name to a set of items a
  player can request after the cooldown has expired.
*/
@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KitModel extends APersistentModel {

  @Getter
  @Setter
  @NonNull
  @ModelProperty(isUnique = true)
  private String name;

  @Getter
  @Setter
  @NonNull
  @ModelProperty
  private Inventory items;

  @Getter
  @Setter
  @NonNull
  @ModelProperty
  private int cooldownSeconds;
}
