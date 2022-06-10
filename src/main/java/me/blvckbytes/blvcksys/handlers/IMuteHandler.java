package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.MuteModel;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Public interfaces which the mute handler provides to other consumers.
 */
public interface IMuteHandler {

  /**
   * Cast a new mute onto a target player
   * @param creator Mute creator
   * @param target Target to be muted
   * @param durationSeconds Duration in seconds
   * @param reason Reason of this mute, null means none
   * @return The mute that has just been created
   */
  MuteModel createMute(
    OfflinePlayer creator,
    OfflinePlayer target,
    Integer durationSeconds,
    @Nullable String reason
  ) throws PersistenceException;

  /**
   * Broadcast a mute in the chat to all online players
   * @param mute Mute to broadcast
   */
  void broadcastMute(MuteModel mute);

  /**
   * List all mutes of a player based on a set of constraints
   * @param target Target player
   * @param isRevoked Whether the mute should have been revoked, null means doesn't matter
   * @param isActive Whether the mute should be currently active, null means doesn't matter
   * @return List of mutes that meet the constraints
   */
  List<MuteModel> listMutes(
    OfflinePlayer target,
    @Nullable Boolean isRevoked,
    @Nullable Boolean isActive
  ) throws PersistenceException;

  /**
   * Check whether or not a player is currently muted
   * @param target Target player
   * @return The active mute if the player's muted, empty if the player's free to chat
   */
  Optional<MuteModel> isCurrentlyMuted(OfflinePlayer target);

  /**
   * Build a map of variables to be imported when creating
   * config messages in regards to a specific mute model
   * @param mute Mute model to use as a variable value supplier
   * @return Variable map
   */
  Map<String, String> buildMuteVariables(MuteModel mute);

  /**
   * Find a specific mute by it's ID
   * @param id ID of the target mute
   * @return Mute model, empty if there was no such ID
   */
  Optional<MuteModel> findById(UUID id);

  /**
   * Revoke an existing mute
   * @param mute Mute to revoke
   * @param revoker Player revoking the mute
   * @param reason Reason of the revocation
   * @return The revoked model or null if the model has already been revoked
   */
  MuteModel revokeMute(MuteModel mute, Player revoker, @Nullable String reason);

  /**
   * Broadcast a mute's revocation in the chat to all online players
   * @param mute Revoked mute to broadcast
   */
  void broadcastRevoke(MuteModel mute);

  /**
   * Build a mute screen from the parameters of an active mute
   * @param mute Active ban
   * @return Formatted and customized mute screen
   */
  String buildMuteScreen(MuteModel mute);

  /**
   * Delete a mute permanently
   * @param mute Mute to delete
   * @return SUCC on deletion, ERR if the mute is still active, EMPTY if the mute didn't exist
   */
  TriResult deleteMute(MuteModel mute);
}
