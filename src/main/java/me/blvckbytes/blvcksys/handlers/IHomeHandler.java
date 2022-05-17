package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.HomeModel;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Public interfaces which the home handler provides to other consumers.
 */
public interface IHomeHandler {

  /**
   * Create a new home at a given location, with a given name
   * @param creator creator of this new home
   * @param name Name of the home, has to be unique for this creator
   * @param loc Location of the home
   * @return Newly created home, empty on name collisions
   */
  Optional<HomeModel> createHome(OfflinePlayer creator, String name, Location loc) throws PersistenceException;

  /**
   * Update an existing home by it's name and set a new location
   * @param creator creator of this home
   * @param name Name of the home
   * @param loc New location of the home
   * @return True on success, false if no home with this name existed
   */
  boolean updateLocation(OfflinePlayer creator, String name, Location loc) throws PersistenceException;

  /**
   * Delete an existing home by it's name
   * @param creator creator of this home
   * @param name Name of the home
   * @return True on success, false if no home with this name existed
   */
  boolean deleteHome(OfflinePlayer creator, String name) throws PersistenceException;

  /**
   * Count all existing homes of a creator
   * @param creator creator of the homes to count
   * @return Number of existing homes
   */
  int countHomes(OfflinePlayer creator) throws PersistenceException;

  /**
   * List all existing homes a creator owns
   * @param creator creator of the homes to list
   * @return List of existing homes
   */
  List<HomeModel> listHomes(OfflinePlayer creator) throws PersistenceException;

  /**
   * Find a home of a specific player by it's name
   * @param creator Creator of the home
   * @param name Name of the home
   * @return Home if it exists, empty if there was no such home
   */
  Optional<HomeModel> findHome(OfflinePlayer creator, String name) throws PersistenceException;
}
