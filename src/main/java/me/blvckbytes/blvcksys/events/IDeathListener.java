package me.blvckbytes.blvcksys.events;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Public interfaces which the death listener provides to other consumers.
*/
public interface IDeathListener {

  /**
   * Handle the death of a player by broadcasting, spawning animations and the like
   * @param victim Player that just died
   * @param killer Player that killed the victim, null means died by themselves
   */
  void handleDeath(Player victim, @Nullable Player killer);
}
