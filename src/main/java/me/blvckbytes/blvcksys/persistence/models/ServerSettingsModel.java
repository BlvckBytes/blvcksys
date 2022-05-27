package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Stores all settings which regard the server as a whole
  instead of specific players.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerSettingsModel extends APersistentModel {

  public static final int DEFAULT_ATTACK_SPEED = 20;

  @ModelProperty
  private int attackSpeed;

  public static ServerSettingsModel createDefault() {
    return new ServerSettingsModel(DEFAULT_ATTACK_SPEED);
  }
}
