package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.MigrationDefault;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  All preferences players can customize for themselves.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PreferencesModel extends APersistentModel {

  @ModelProperty
  private OfflinePlayer owner;

  @ModelProperty(migrationDefault = MigrationDefault.FALSE)
  private boolean scoreboardHidden;

  @ModelProperty(migrationDefault = MigrationDefault.FALSE)
  private boolean chatHidden;
}
