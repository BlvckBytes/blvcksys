package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/03/2022

  A fake armor stand that is spawned for clients through packets only.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ArmorStandModel extends APersistentModel {

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty(isUnique = true)
  private String name;

  @ModelProperty
  private Location loc;
}
