package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.HologramLineModel;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Public interfaces which the hologram handler provides to other consumers.
 */
public interface IHologramHandler {

  /**
   * Delete a hologram by deleting all of it's lines
   * @param name Name of the target hologram
   * @return True on success, false if there was no hologram with this name
   */
  boolean deleteHologram(String name) throws PersistenceException;

  /**
   * Delete a single hologram line and update it's neighbors links
   * @param line Hologram line to delete
   * @return True on success, false if there was no hologram line that matched the model
   */
  boolean deleteHologramLine(HologramLineModel line) throws PersistenceException;

  /**
   * Create a new hologram line and populate it's links so it's added as a last node
   * @param name Name of the hologram
   * @param loc Location of the line
   * @param text Text of the line
   * @return Created model
   */
  HologramLineModel createHologramLine(String name, Location loc, String text) throws PersistenceException;

  /**
   * Get all lines a hologram holds
   * @param name Name of the hologram
   * @return Optional list of lines, empty if this hologram didn't yet exist
   */
  Optional<List<HologramLineModel>> getHologramLines(String name) throws PersistenceException;
}
