package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ForeignKeyAction;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Saves the account of keys for a player in regards to
  a specific crate.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CrateKeyModel extends APersistentModel {

  @ModelProperty(isUnique = true)
  private OfflinePlayer owner;

  @ModelProperty(
    foreignKey = CrateModel.class,
    foreignChanges = ForeignKeyAction.DELETE_CASCADE,
    isUnique = true
  )
  private UUID crateId;

  @ModelProperty
  private int numberOfKeys;
}
