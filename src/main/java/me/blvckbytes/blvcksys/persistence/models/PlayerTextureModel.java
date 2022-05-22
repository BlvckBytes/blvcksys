package me.blvckbytes.blvcksys.persistence.models;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Stores a player's skin texutres and their UUID.
*/
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

  /**
   * Convert the properties into a GameProfile
   */
  public GameProfile toProfile() {
    GameProfile profile = new GameProfile(uuid, name);
    profile.getProperties().put("textures", new Property("textures", textures));
    return profile;
  }
}
