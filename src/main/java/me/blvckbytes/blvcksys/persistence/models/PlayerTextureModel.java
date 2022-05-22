package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerTextureModel extends APersistentModel {

  @ModelProperty(isUnique = true)
  String name;

  @ModelProperty(isUnique = true)
  UUID uuid;

  @ModelProperty
  String textures;
}
