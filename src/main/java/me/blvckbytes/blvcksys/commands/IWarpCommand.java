package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/23/2022

  Public interfaces which the warp command provides to other consumers.
*/
public interface IWarpCommand {

  /**
   * Invoke a warping process programmatically
   * @param p Player to warp
   * @param name Name of the warp
   * @param done Optional callback which is invoked after the teleportation
   * @return True on success, false if the warp didn't exist
   */
  boolean invokeWarping(Player p, String name, @Nullable Runnable done) throws PersistenceException;
}
