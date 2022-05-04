package me.blvckbytes.blvcksys.persistence.models;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.blvckbytes.blvcksys.persistence.ModelIdentifier;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import me.blvckbytes.blvcksys.persistence.PersistentModel;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  A warping-point that corresponds a unique name to a
  specific point within a specific world.
*/
@RequiredArgsConstructor
@PersistentModel(name = "warp")
public class WarpModel extends APersistentModel<WarpModel> {

  @Getter
  @ModelIdentifier
  private UUID id;

  @Getter
  @Setter
  @NonNull
  @ModelProperty(isUnique = true)
  private String name;

  @Getter
  @Setter
  @NonNull
  @ModelProperty
  private Location loc;

  @Getter
  @Setter
  @NonNull
  @ModelProperty
  private OfflinePlayer creator;
}
