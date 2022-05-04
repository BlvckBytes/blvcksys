package me.blvckbytes.blvcksys.persistence.models;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.blvckbytes.blvcksys.persistence.ModelProperty;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  An offline player where only their UUID is stored for revival,
  used with the OfflinePlayerTransformer.
*/
@RequiredArgsConstructor
public class OfflinePlayerModel extends APersistentModel {

  @Getter
  @Setter
  @NonNull
  @ModelProperty
  private UUID uuid;
}
