package me.blvckbytes.blvcksys.packets;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Public interfaces to handle the registration process of a packet modifier.
*/
public interface IPacketInterceptor {

  /**
   * Register a new modifier for all players
   * @param modifier Packet modifier to register
   * @param priority Priority of this modifier
   */
  void register(IPacketModifier modifier, ModificationPriority priority);

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
   * @param priority Priority of this modifier
   */
  void registerSpecific(UUID target, IPacketModifier modifier, ModificationPriority priority);

  /**
   * Unegister an existing modifier for a specific player
   * @param target Player targetted with this modifier
   * @param modifier Packet modifier to unregister
   */
  void unregisterSpecific(UUID target, IPacketModifier modifier);

  /**
   * Check if a specific modifier is already registered
   * @param target Player targetted with this modifier
   * @param modifier Modifier to check
   * @return True if registered, false otherwise
   */
  boolean isRegisteredSpecific(UUID target, IPacketModifier modifier);
}
