package me.blvckbytes.blvcksys.packets.modifiers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerListHeaderFooter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

// TODO: PacketPlayOutScoreboardTeam

@AutoConstruct
public class TabListPacketModifier implements IPacketModifier, Listener, IAutoConstructed {

  private final IConfig cfg;
  private final MCReflect refl;

  public TabListPacketModifier(
    @AutoInject IPacketInterceptor interceptor,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    this.cfg = cfg;
    this.refl = refl;
    interceptor.register(this);
  }

  //=========================================================================//
  //                                Modifiers                                //
  //=========================================================================//

  @Override
  public Packet<?> modifyIncoming(Player sender, NetworkManager nm, Packet<?> incoming) {
    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(Player receiver, NetworkManager nm, Packet<?> outgoing) {
    // Override this packet if it occurs naturally
    if (outgoing instanceof PacketPlayOutPlayerListHeaderFooter)
      return generateTabHeaderFooter(receiver);

    return outgoing;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void cleanup() {}

  @Override
  public void initialize() {
    updateAllHeaderFooter();
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    updateAllHeaderFooter();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    updateAllHeaderFooter();
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Update the header and footer for all online players
   */
  private void updateAllHeaderFooter() {
    for (Player t : Bukkit.getOnlinePlayers())
      refl.sendPacket(t, generateTabHeaderFooter(t));
  }

  /**
   * Generate a player-specific tablist header and footer packet
   * @param p Receiving player
   * @return Custom generated packet
   */
  private Packet<?> generateTabHeaderFooter(Player p) {
    return new PacketPlayOutPlayerListHeaderFooter(
      new ChatComponentText(cfg.get(ConfigKey.TABLIST_HEADER)),
      new ChatComponentText(cfg.get(ConfigKey.TABLIST_FOOTER))
    );
  }
}
