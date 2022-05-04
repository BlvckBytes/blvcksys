package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelIdentifier;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import me.blvckbytes.blvcksys.persistence.PersistentModel;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  A specific position within a world of the server.
*/
@RequiredArgsConstructor
@PersistentModel(name = "location")
public class LocationModel extends APersistentModel<LocationModel> {

  @Getter
  @ModelIdentifier
  private UUID id;

  @Getter
  @Setter
  @NonNull
  @ModelProperty
  private double x, y, z, yaw, pitch;

  @Getter
  @Setter
  @NonNull
  @ModelProperty
  private String world;
}
