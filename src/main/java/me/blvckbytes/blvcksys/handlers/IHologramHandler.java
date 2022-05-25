package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.HologramLineModel;
import me.blvckbytes.blvcksys.persistence.models.SequenceSortResult;
import net.minecraft.util.Tuple;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Public interfaces which the hologram handler provides to other consumers.
 */
public interface IHologramHandler {

  /**
   * Sort all lines of an existing hologram to the sequence of the
   * provided line IDs, where 0 is the first line and n is the last
   * line, handled as currently stored in persistence. All n IDs
   * have to be present for this action to result in a success.
   * @param name Name of the hologram
   * @param lineIdSequence Sequence of line-IDs in the desired order
   * @return Zero on success, number of missing IDs when missing IDs
   */
  Tuple<SequenceSortResult, Integer> sortHologramLines(String name, int[] lineIdSequence) throws PersistenceException;

  /**
   * Delete a hologram by deleting all of it's lines
   * @param name Name of the target hologram
   * @return True on success, false if there was no hologram with this name
   */
  boolean deleteHologram(String name) throws PersistenceException;

  /**
   * Move a hologram by moving all of it's lines
   * @param name Name of the target hologram
   * @param loc Location to move to
   * @return True on success, false if there was no hologram with this name
   */
  boolean moveHologram(String name, Location loc) throws PersistenceException;

  /**
   * Delete a single hologram line and update it's neighbors links
   * @param line Hologram line to delete
   * @return True on success, false if there was no hologram line that matched the model
   */
  boolean deleteHologramLine(HologramLineModel line) throws PersistenceException;

  /**
   * Create a new hologram line and populate it's links so it's added as a last node
   * @param creator Player creating this hologram
   * @param name Name of the hologram
   * @param loc Location of the line
   * @param text Text of the line
   * @return Created model
   */
  HologramLineModel createHologramLine(OfflinePlayer creator, String name, Location loc, String text) throws PersistenceException;

  /**
   * Change a hologram line's text content
   * @param line Line to change
   * @param newLine New line contents
   */
  void changeHologramLine(HologramLineModel line, String newLine);

  /**
   * Get all lines a hologram holds
   * @param name Name of the hologram
   * @return Optional list of lines, empty if this hologram didn't yet exist
   */
  Optional<List<HologramLineModel>> getHologramLines(String name) throws PersistenceException;

  /**
   * Get a list of holograms (map from name to lines) which are near a specified location
   * @param where Where to search at
   * @param rangeRadius Max. distance between where and the hologram
   * @return List of holograms that are within the radius
   */
  Map<String, List<HologramLineModel>> getNear(Location where, double rangeRadius) throws PersistenceException;

  /**
   * Creates a new temporary hologram which is not persisted
   * @param loc Location of the hologram
   * @param lines List of lines to display
   */
  MultilineHologram createTemporary(Location loc, List<String> lines);

  /**
   * Destroys an existing temporary hologram
   * @param hologram Hologram handle
   */
  void destroyTemporary(MultilineHologram hologram);
}
