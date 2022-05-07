package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

import java.util.Date;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Saves a cooldown which has an expiry stamp and an identifying
  token for a holding player. Tokens represent a way of connecting the
  cooldown session to a resource and it's properties at which the
  cooldown is targetted at. See {@link ACooldownModel}
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CooldownSessionModel extends APersistentModel {

  @ModelProperty
  private OfflinePlayer holder;

  @ModelProperty
  private Date expiresAt;

  @ModelProperty
  private String token;
}