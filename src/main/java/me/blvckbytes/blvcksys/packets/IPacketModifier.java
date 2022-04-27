package me.blvckbytes.blvcksys.packets;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Represents a class that can modify in- and outgoing packets
  and will be registered within a chain of modifiers
*/
public interface IPacketModifier {

  /**
   * Interception method to modify incoming packets
   * @param sender UUID of the player who's client sent the packet (can be null if not yet connected)
   * @param nm NetworkManager corresponding to the requesting client
   * @param incoming Incoming packet
   * @return Modified incoming packet, null to terminate the packet
   */
  Packet<?> modifyIncoming(UUID sender, NetworkManager nm, Packet<?> incoming);

  /**
   * Interception method to modify outgoing packets
   * @param receiver UUID of the player who's client will receive the packet (can be null if not yet connected)
   * @param nm NetworkManager corresponding to the requesting client
   * @param outgoing Outgoing packet
   * @return Modified outgoing packet, null to terminate the packet
   */
  Packet<?> modifyOutgoing(UUID receiver, NetworkManager nm, Packet<?> outgoing);
}
