package me.blvckbytes.blvcksys.packets;

import net.minecraft.network.protocol.Packet;
import org.bukkit.entity.Player;

public interface IPacketModifier {

  /**
   * Interception method to modify incoming packets
   * @param sender Player who's client sent the packet
   * @param incoming Incoming packet
   * @return Modified incoming packet, null to terminate the packet
   */
  Packet<?> modifyIncoming(Player sender, Packet<?> incoming);

  /**
   * Interception method to modify outgoing packets
   * @param receiver Player who's client will receive the packet
   * @param outgoing Outgoing packet
   * @return Modified outgoing packet, null to terminate the packet
   */
  Packet<?> modifyOutgoing(Player receiver, Packet<?> outgoing);
}
