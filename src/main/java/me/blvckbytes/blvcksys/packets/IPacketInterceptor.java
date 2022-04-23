package me.blvckbytes.blvcksys.packets;

import org.bukkit.entity.Player;

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
  void registerSpecific(Player target, IPacketModifier modifier);

  /**
   * Unegister an existing modifier for a specific player
   * @param target Player targetted with this modifier
   * @param modifier Packet modifier to unregister
   */
  void unregisterSpecific(Player target, IPacketModifier modifier);

  /**
   * Check if a specific modifier is already registered
   * @param target Player targetted with this modifier
   * @param modifier Modifier to check
   * @return True if registered, false otherwise
   */
  boolean isRegisteredSpecific(Player target, IPacketModifier modifier);
}
