package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.packets.communicators.armorstand.ArmorStandProperties;
import me.blvckbytes.blvcksys.persistence.models.ArmorStandModel;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
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
   * Gets an armor stand's properties
   * @param name Name of the target armor stand
   * @return Stand's properties, empty if there was no armor stand with this name
   */
  Optional<ArmorStandProperties> getProperties(String name);

  /**
   * Sets an armor stands properties
   * @param name Name of the target armor stand
   * @param properties Properties to set
   * @param store Whether to store the properties, use false for intermediate values while configuring
   * @return True on success, false if there was no armor stand with this name
   */
  boolean setProperties(String name, ArmorStandProperties properties, boolean store);

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

  /**
   * Creates a new temporary armor stand which is not persisted
   * @param loc Location of the armor stand
   * @param recipients List of recipients, null means all players
   * @param properties Initial properties to spawn with
   */
  FakeArmorStand createTemporary(Location loc, @Nullable Collection<? extends Player> recipients, ArmorStandProperties properties);

  /**
   * Destroys an existing temporary armor stand
   * @param stand Armor stand handle
   */
  void destroyTemporary(FakeArmorStand stand);
}
