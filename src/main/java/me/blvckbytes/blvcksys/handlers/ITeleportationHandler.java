package me.blvckbytes.blvcksys.handlers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Public interfaces which the teleportation handler provides to other consumers.
 */
public interface ITeleportationHandler {

  /**
   * Request a new teleportation to a specific location
   * @param p Player that wants to teleport themselves
   * @param to Location to teleport to
   * @param done Optional callback which is invoked after the teleportation
   * @param cancelled Optional callback which is invoked on cancellation
   */
  void requestTeleportation(Player p, Location to, @Nullable Runnable done, @Nullable Runnable cancelled);
}
