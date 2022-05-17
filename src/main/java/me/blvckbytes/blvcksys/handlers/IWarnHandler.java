package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.WarnModel;
import net.minecraft.util.Tuple;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Public interfaces which the warn handler provides to other consumers.
 */
public interface IWarnHandler {

  /**
   * Cast a new warn onto a target player
   * @param creator Warn creator
   * @param target Target to be warned
   * @param durationSeconds Duration in seconds
   * @param reason Reason of this warn, null means none
   * @return The warn that has just been created, empty if the
   * player has reached the maximum number of warns
   */
  Optional<WarnModel> createWarn(
    OfflinePlayer creator,
    OfflinePlayer target,
    Integer durationSeconds,
    @Nullable String reason
  ) throws PersistenceException;

  /**
   * Broadcast a warn in the chat to all online players
   * @param warn Warn to broadcast
   */
  void broadcastWarn(WarnModel warn);

  /**
   * List all warns of a player based on a set of constraints
   * @param target Target player
   * @param isPermanent Whether warns should be permanent, null means doesn't matter
   * @param isRevoked Whether the warn should have been revoked, null means doesn't matter
   * @param isActive Whether the warn should be currently active, null means doesn't matter
   * @return List of warns that meet the constraints
   */
  List<WarnModel> listWarns(
    OfflinePlayer target,
    @Nullable Boolean isPermanent,
    @Nullable Boolean isRevoked,
    @Nullable Boolean isActive
  ) throws PersistenceException;

  /**
   * Build a map of variables to be imported when creating
   * config messages in regards to a specific warn model
   * @param warn Warn model to use as a variable value supplier
   * @return Variable map
   */
  Map<String, Tuple<Pattern, String>> buildWarnVariables(WarnModel warn);

  /**
   * Find a specific warn by it's ID
   * @param id ID of the target warn
   * @return Warn model, empty if there was no such ID
   */
  Optional<WarnModel> findById(UUID id);

  /**
   * Revoke an existing warn
   * @param warn Warn to revoke
   * @param revoker Player revoking the warn
   * @param reason Reason of the revocation
   * @return The revoked model or null if the model has already been revoked
   */
  WarnModel revokeWarn(WarnModel warn, Player revoker, @Nullable String reason);

  /**
   * Broadcast a warn's revocation in the chat to all online players
   * @param warn Revoked warn to broadcast
   */
  void broadcastRevoke(WarnModel warn);

  /**
   * Count the number of currently active warns of a player
   * @param target Target player
   * @return Number of active warns
   */
  int countActiveWarns(OfflinePlayer target);

  /**
   * Get the maximum number of active warns a player may
   * have at any point in time before punishment occurs
   */
  int getMaxActiveWarns();
}
