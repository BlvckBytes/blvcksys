package me.blvckbytes.blvcksys.handlers;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/03/2022

  Describes an entity that requires a cooldown.
*/
public interface ICooldownable {

  /**
   * Generates the token used to identify this cooldownable
   */
  String generateToken();

  /**
   * Get the duration of this cooldown in seconds
   */
  int getDurationSeconds();

}
