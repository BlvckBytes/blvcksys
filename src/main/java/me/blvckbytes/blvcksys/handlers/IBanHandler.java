package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.BanModel;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/13/2022

  Public interfaces which the ban handler provides to other consumers.
 */
public interface IBanHandler {

  /**
   * Cast a new ban onto a target player
   * @param creator Ban creator
   * @param target Target to be banned
   * @param durationSeconds Duration in seconds, null means permanent
   * @param ipAddress Target internet address, null means none
   * @param reason Reason of this ban, null means none
   * @return The ban that has just been created
   */
  BanModel createBan(
    OfflinePlayer creator,
    OfflinePlayer target,
    @Nullable Integer durationSeconds,
    @Nullable String ipAddress,
    @Nullable String reason
  ) throws PersistenceException;

  /**
   * Broadcast a ban in the chat to all online players
   * @param ban Ban to broadcast
   */
  void broadcastBan(BanModel ban);

  /**
   * List all bans of a player based on a set of constraints
   * @param target Target player
   * @param isPermanent Whether bans should be permanent, null means doesn't matter
   * @param hasIpAddress Whether an address should be stored with the ban, null means doesn't matter
   * @param isRevoked Whether the ban should have been revoked, null means doesn't matter
   * @param isActive Whether the ban should be currently active, null means doesn't matter
   * @return List of bans that meet the constraints
   */
  List<BanModel> listBans(
    OfflinePlayer target,
    @Nullable Boolean isPermanent,
    @Nullable Boolean hasIpAddress,
    @Nullable Boolean isRevoked,
    @Nullable Boolean isActive
  ) throws PersistenceException;

  /**
   * Check whether or not a player is currently banned
   * @param target Target player
   * @param addr Address to check against as well
   * @return The active ban if the player's banned, empty if the player's free to join
   */
  Optional<BanModel> isCurrentlyBanned(OfflinePlayer target, @Nullable InetAddress addr);
}
