package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Location;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Saves a single line of a hologram and corresponds that to
  it's name as well as a location. Lines are kept in order by a
  doubly linked list, where lines point at their successor
  as well as their predecessor.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HologramLineModel extends APersistentModel {

  @ModelProperty
  private String name;

  @ModelProperty
  private Location loc;

  @ModelProperty
  private String text;

  @ModelProperty(foreignKey = HologramLineModel.class, isNullable = true)
  private UUID previousLine;

  @ModelProperty(foreignKey = HologramLineModel.class, isNullable = true)
  private UUID nextLine;
}
