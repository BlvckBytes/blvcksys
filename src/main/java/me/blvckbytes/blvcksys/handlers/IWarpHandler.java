package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/26/2022

  Public interfaces which the warp handler provides to other consumers.
 */
public interface IWarpHandler {

  /**
   * Get a warp by it's name
   * @param name Name of the warp
   * @return Optional warp, empty if there was no such named warp
   */
  Optional<WarpModel> getWarp(String name) throws PersistenceException;

  /**
   * Set a new warping point
   * @param name Name of the warp
   * @return Success state, false if there already was a warp with this name
   */
  boolean setWarp(String name, OfflinePlayer creator, Location loc) throws PersistenceException;

  /**
   * Move an existing warping point
   * @param name Name of the warp
   * @return Success state, false if there was no such named warp
   */
  boolean moveWarp(String name, OfflinePlayer creator, Location loc) throws PersistenceException;

  /**
   * Delete an existing warping point
   * @param name Name of the warp
   * @return Success state, false if there was no such named warp
   */
  boolean deleteWarp(String name) throws PersistenceException;
}
