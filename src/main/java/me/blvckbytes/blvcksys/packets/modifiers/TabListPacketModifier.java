package me.blvckbytes.blvcksys.packets.modifiers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Optional;

// TODO: PacketPlayOutScoreboardTeam

@AutoConstruct
public class TabListPacketModifier implements IPacketModifier, Listener, IAutoConstructed {

  private final IConfig cfg;
  private final MCReflect refl;
  private final ILogger logger;

  public TabListPacketModifier(
    @AutoInject IPacketInterceptor interceptor,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
  ) {
    this.cfg = cfg;
    this.refl = refl;
    this.logger = logger;

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
    // Override header and footer packets
    if (outgoing instanceof PacketPlayOutPlayerListHeaderFooter)
      return generateTabHeaderFooter(receiver);

    // Override scoreboard team packets
    if (outgoing instanceof PacketPlayOutScoreboardTeam)
      return outgoing;

    return outgoing;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void cleanup() {}

  @Override
  public void initialize() {
    updateAll();
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    updateAll();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    updateAll();
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Update the header and footer for all online players
   */
  private void updateAll() {
    for (Player t : Bukkit.getOnlinePlayers()) {
      refl.sendPacket(t, generateTabHeaderFooter(t));
      generateScoreboardTeam(t).ifPresent(p -> {
        System.out.println("sending scoreboard");
        refl.sendPacket(t, p);
      });
    }
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

  /**
   * Get a EnumChatFormat by it's color character
   * @param color Color character
   * @return Result or empty on errors
   */
  private Optional<EnumChatFormat> chatFormatFromColor(char color) {
    // Loop all enum values
    for (EnumChatFormat cf : EnumChatFormat.values()) {
      Optional<Object> colorCode = refl.getFieldByType(cf, char.class);

      // Could not get the color code of this enum entry
      if (colorCode.isEmpty())
        continue;

      // Color char matches
      if (((char) colorCode.get()) == color)
        return Optional.of(cf);
    }

    // Not found
    return Optional.empty();
  }

  private Optional<Packet<?>> generateScoreboardTeam(Player p) {
    return refl.createGarbageInstance(PacketPlayOutScoreboardTeam.b.class)
      .flatMap(b -> {

        refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage("1"), 0);  // Team display name
        refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage("§c[Prefix] "), 1);  // Prefix
        refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage(" §b[Suffix]"), 2);  // Suffix
        refl.setFieldByType(b, String.class, "always", 0); // Name tag visibility: always, hideForOtherTeams, hideForOwnTeam, never
        refl.setFieldByType(b, String.class, "always", 1); // Collision rule: always, pushOtherTeams, pushOwnTeam, never

        chatFormatFromColor('9').ifPresent(ecf -> {
          refl.setFieldByType(b, EnumChatFormat.class, ecf, 0); // Player name color
        });

        refl.setFieldByType(b, int.class, 0x02, 0); // Bit mask. 0x01: Allow friendly fire, 0x02: can see invisible players on same team.

        return refl.invokeConstructor(
          PacketPlayOutScoreboardTeam.class,
          "A",                  // Unique team name
          0,                    // Mode (0=create, 1=remove, 2=update, 3=add entites, 4=remove entities)
          Optional.of(b),       // Optional team (not needed for (1 | 3 | 4), I guess?)
          Bukkit.getOnlinePlayers().stream().map(Player::getName).toList()  // Names of all team members
        ).map(o -> {
          logger.logDebug(o, 5);
          return (Packet<?>) o;
        });
      });
  }
}
