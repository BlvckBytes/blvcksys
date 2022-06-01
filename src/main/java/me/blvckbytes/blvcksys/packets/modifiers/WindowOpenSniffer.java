package me.blvckbytes.blvcksys.packets.modifiers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.packets.ModificationPriority;
import me.blvckbytes.blvcksys.packets.PacketSource;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Watches outgoing window opening packets and caches the last window
  ID for every player, as long as they're online, for quick access.
*/
@AutoConstruct
public class WindowOpenSniffer implements IPacketModifier, Listener {

  private final Map<Player, Integer> lastWindowIds;
  private final MCReflect refl;
  private final ILogger logger;

  public WindowOpenSniffer(
    @AutoInject IPacketInterceptor packetInterceptor,
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
  ) {
    this.lastWindowIds = Collections.synchronizedMap(new HashMap<>());
    this.refl = refl;
    this.logger = logger;

    packetInterceptor.register(this, ModificationPriority.LOW);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  /**
   * Get the player's currently open top window ID
   * @param p Target player
   * @return Window ID
   */
  public Integer getTopInventoryWindowId(Player p) {
    return lastWindowIds.getOrDefault(p, 0);
  }

  //=========================================================================//
  //                                Modifiers                                //
  //=========================================================================//

  @Override
  public Packet<?> modifyIncoming(UUID sender, PacketSource ps, Packet<?> incoming) {
    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(UUID receiver, NetworkManager nm, Packet<?> outgoing) {
    Player p = Bukkit.getPlayer(receiver);

    if (outgoing instanceof PacketPlayOutOpenWindow ow && p != null) {
      try {
        lastWindowIds.put(p, refl.getFieldByType(ow, int.class, 0));
      } catch (Exception e) {
        logger.logError(e);
      }
    }

    return outgoing;
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    lastWindowIds.remove(e.getPlayer());
  }
}
