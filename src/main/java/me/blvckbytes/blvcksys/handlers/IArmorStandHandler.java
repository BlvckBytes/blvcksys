package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.models.ArmorStandModel;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/03/2022

  Public interfaces which the armor stand handler provides to other consumers.
*/
public interface IArmorStandHandler {

  /**
   * Creates a new armor stand
   * @param creator Creator of this armor stand
   * @param name Name of the armor stand
   * @param loc Location of the armor stand
   * @return Newly created model on success, empty if the name already exists
   */
  Optional<ArmorStandModel> create(OfflinePlayer creator, String name, Location loc);

  /**
   * Deletes an existing armor stand
   * @param name Name of the target armor stand
   * @return True on success, false if there was no armor stand with this name
   */
  boolean delete(String name);

  /**
   * Move an existing armor stand
   * @param name Name of the target armor stand
   * @return True on success, false if there was no armor stand with this name
   */
  boolean move(String name, Location loc);

  /**
   * Get an armor stand by it's name
   * @param name Name of the target armor stand
   * @return Armor stand on success, false if there was no armor stand with this name
   */
  Optional<ArmorStandModel> getByName(String name);

  /**
   * Get an armor stand by it's location
   * @param loc Location of the target armor stand
   * @param radius Radius to search in, relative to the location
   * @return Armor stand on success, false if there was no armor stand at this location within the radius
   */
  Optional<ArmorStandModel> getByLocation(Location loc, int radius);
}
