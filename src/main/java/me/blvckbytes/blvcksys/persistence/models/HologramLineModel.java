package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ForeignKeyAction;
import me.blvckbytes.blvcksys.persistence.MigrationDefault;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HologramLineModel extends ASequencedModel<HologramLineModel> {

  @ModelProperty(migrationDefault = MigrationDefault.NULL)
  private OfflinePlayer creator;

  @ModelProperty
  private String name;

  @ModelProperty
  private Location loc;

  @ModelProperty
  private String text;

  public HologramLineModel(
    OfflinePlayer creator,
    String name,
    Location loc,
    String text,
    UUID previous,
    UUID next
  ) {
    super(previous, next);

    this.creator = creator;
    this.name = name;
    this.loc = loc;
    this.text = text;
  }
}
