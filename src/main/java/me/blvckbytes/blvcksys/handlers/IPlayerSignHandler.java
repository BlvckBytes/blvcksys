package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.models.PlayerSignModel;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  Public interfaces which the player sign handler provides to other consumers.
*/
public interface IPlayerSignHandler {

  /**
   * Create a new managed player sign
   * @param creator Executing player
   * @param sign Sign to create
   * @return The created sign, empty if there's already a sign at that location
   */
  Optional<PlayerSignModel> createSign(OfflinePlayer creator, Sign sign);

  /**
   * Find a sign's connected sign model
   * @param sign Sign to look up
   * @return Sign model, empty if the sign is unmanaged
   */
  Optional<PlayerSignModel> findSign(Sign sign);

  /**
   * Edit a managed player sign's line
   * @param editor Executing player
   * @param sign Sign to edit
   * @param line Line contents
   * @param lineIndex Index of the line to edit (1-4)
   * @return True on success, false if there was no sign at this location
   */
  boolean editSign(OfflinePlayer editor, Sign sign, String line, int lineIndex);

  /**
   * Move a managed sign from one location to another
   * @param editor Executing player
   * @param from Existing sign
   * @param to Sign to transfer to
   * @return SUCC on success, ERR if "to" is already occupied, EMPTY if "from" doesn't exist
   */
  TriResult moveSign(OfflinePlayer editor, Sign from, Sign to);

  /**
   * Delete an existing managed player sign by it's location
   * @param sign Existing sign to delete
   * @return True on success, false if there was no sign at this location
   */
  boolean deleteSign(Sign sign);
}
