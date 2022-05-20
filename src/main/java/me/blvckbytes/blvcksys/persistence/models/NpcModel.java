package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/20/2022

  A fake NPC that is spawned for clients through packets only and
  act as means of interaction with several features.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NpcModel extends APersistentModel {

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty(isUnique = true)
  private String name;

  @ModelProperty
  private Location loc;

  @ModelProperty(isNullable = true)
  private String skin;

}
