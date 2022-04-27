package me.blvckbytes.blvcksys.packets;

import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Public interfaces to handle the registration process of a packet modifier.
*/
public interface IPacketInterceptor {

  /**
   * Register a new modifier for all players
   * @param modifier Packet modifier to register
   */
  void register(IPacketModifier modifier);

  /**
   * Unregister an existing modifier for all players
   * @param modifier Packet modifier to unregister
   */
  void unregister(IPacketModifier modifier);

  /**
   * Check if a modifier is already registered
   * @param modifier Modifier to check
   * @return True if registered, false otherwise
   */
  boolean isRegistered(IPacketModifier modifier);

  /**
   * Register a new modifier for a specific player
   * @param target Player to target with this modifier
   * @param modifier Packet modifier to register
   */
  void registerSpecific(OfflinePlayer target, IPacketModifier modifier);

  /**
   * Unegister an existing modifier for a specific player
   * @param target Player targetted with this modifier
   * @param modifier Packet modifier to unregister
   */
  void unregisterSpecific(OfflinePlayer target, IPacketModifier modifier);

  /**
   * Check if a specific modifier is already registered
   * @param target Player targetted with this modifier
   * @param modifier Modifier to check
   * @return True if registered, false otherwise
   */
  boolean isRegisteredSpecific(OfflinePlayer target, IPacketModifier modifier);
}
