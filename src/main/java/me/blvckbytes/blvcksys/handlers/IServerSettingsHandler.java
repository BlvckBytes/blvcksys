package me.blvckbytes.blvcksys.handlers;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Public interfaces which the settings handler provides to other consumers.
 */
public interface IServerSettingsHandler {

  /**
   * Get the currently configured attack speed
   */
  int getAttackSpeed();

  /**
   * Set the configured attack speed
   * @param value Value to set
   */
  void setAttackSpeed(int value);
}
