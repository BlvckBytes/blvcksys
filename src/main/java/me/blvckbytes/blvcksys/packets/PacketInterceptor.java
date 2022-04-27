package me.blvckbytes.blvcksys.packets;

import io.netty.channel.*;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@AutoConstruct
public class PacketInterceptor implements IPacketInterceptor, Listener, IAutoConstructed {

  // Name of ChannelHandler within the player's pipeline
  private static final String handlerName = "packet_interceptor";

  // List of globally registered modifiers
  private final List<IPacketModifier> globalModifiers;

  // List of per-player registered modifiers
  // Use UUIDs here to allow persistence accross re-joins
  private final Map<UUID, ArrayList<IPacketModifier>> specificModifiers;

  private final ILogger logger;
  private final MCReflect refl;

  public PacketInterceptor(
    @AutoInject ILogger logger,
    @AutoInject MCReflect refl
    ) {
    this.globalModifiers = new ArrayList<>();
    this.specificModifiers = new HashMap<>();
    this.logger = logger;
    this.refl = refl;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void register(IPacketModifier modifier) {
    this.globalModifiers.add(modifier);
  }

  @Override
  public void unregister(IPacketModifier modifier) {
    this.globalModifiers.remove(modifier);
  }

  @Override
  public boolean isRegistered(IPacketModifier modifier) {
    return this.globalModifiers.contains(modifier);
  }

  @Override
  public void registerSpecific(OfflinePlayer target, IPacketModifier modifier) {
    // Create empty list to add to
    if (!this.specificModifiers.containsKey(target.getUniqueId()))
      this.specificModifiers.put(target.getUniqueId(), new ArrayList<>());

    // Add modifier to list
    this.specificModifiers.get(target.getUniqueId()).add(modifier);
  }

  @Override
  public void unregisterSpecific(OfflinePlayer target, IPacketModifier modifier) {
    // Player not even known yet
    if (!this.specificModifiers.containsKey(target.getUniqueId()))
      return;

    // Remove modifier from list
    List<IPacketModifier> modifiers = this.specificModifiers.get(target.getUniqueId());
    modifiers.remove(modifier);

    // Remove from map when no more modifiers remain
    if (modifiers.size() == 0)
      this.specificModifiers.remove(target.getUniqueId());
  }

  @Override
  public boolean isRegisteredSpecific(OfflinePlayer target, IPacketModifier modifier) {
    return this.specificModifiers.getOrDefault(target.getUniqueId(), new ArrayList<>()).contains(modifier);
  }

  @Override
  public void cleanup() {
    // Unregister all globals
    // Loop in reverse to avoid concurrent modifications
    for (int i = this.globalModifiers.size() - 1; i >= 0; i--)
      this.unregister(this.globalModifiers.get(i));

    // Unregister all specifics
    for (Map.Entry<UUID, ArrayList<IPacketModifier>> entry : specificModifiers.entrySet()) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(entry.getKey());

      // Loop in reverse to avoid concurrent modifications
      List<IPacketModifier> modifiers = entry.getValue();
      for (int i = modifiers.size() - 1; i >= 0; i--)
        this.unregisterSpecific(target, modifiers.get(i));
    }

    // Uninject all players before a reload
    for (Player p : Bukkit.getOnlinePlayers())
      uninjectPlayer(p);
  }

  @Override
  public void initialize() {
    // Inject all players after a reload
    for (Player p : Bukkit.getOnlinePlayers())
      injectPlayer(p);
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler(priority = EventPriority.LOWEST)
  public void onJoin(PlayerJoinEvent e) {
    injectPlayer(e.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    uninjectPlayer(e.getPlayer());
  }

  @EventHandler
  public void onPing(ServerListPingEvent e) {
    this.injectAllConnections();
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Remove a previously created injection from the player
   * @param p Target player
   */
  private void uninjectPlayer(Player p) {
    refl.getNetworkChannel(p).ifPresent(nc -> {
      // Remove pipeline entry
      ChannelPipeline pipe = nc.pipeline();

      // Not registered in the pipeline
      if (!pipe.names().contains(handlerName))
        return;

      // Remove handler
      pipe.remove(handlerName);
    });
  }

  /**
   * Create a new injection for a pipeline
   * @param nm NetworkManager instance corresponding to the player, optional (null means not yet connected)
   * @param p Target player, optional (null means not yet connected)
   */
  private void injectChannel(@Nullable Player p, NetworkManager nm, ChannelPipeline pipe) {
    // Already registered in the pipeline
    if (pipe.names().contains(handlerName))
      return;

    // UUID is null for non-player-bound packets
    UUID u = p == null ? null : p.getUniqueId();

    // Create a new channel handler that overrides R/W to intercept
    // This handler gets created in this closure to provide player context
    ChannelDuplexHandler handler = new ChannelDuplexHandler() {

      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Not a packet, not interested
        if (!(msg instanceof Packet<?> packet)) {
          super.channelRead(ctx, msg);
          return;
        }

        // Ensure exceptions don't ruin the pipe
        try {
          // Run through all global modifiers
          for (IPacketModifier modifier : globalModifiers) {
            packet = modifier.modifyIncoming(u, nm, packet);

            // Packet has been terminated
            if (packet == null)
              return;
          }

          // Run through all specific modifiers
          ArrayList<IPacketModifier> specifics = specificModifiers.get(u);
          if (specifics != null) {
            for (IPacketModifier modifier : specifics) {
              packet = modifier.modifyIncoming(u, nm, packet);

              // Packet has been terminated
              if (packet == null)
                return;
            }
          }
        } catch (Exception e) {
          logger.logError(e);
        }

        // Relay modified packet
        super.channelRead(ctx, packet);
      }

      @Override
      public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // Not a packet, not interested
        if (!(msg instanceof Packet<?> packet)) {
          super.write(ctx, msg, promise);
          return;
        }

        // Ensure exceptions don't ruin the pipe
        try {
          // Run through all global modifiers
          for (IPacketModifier modifier : globalModifiers) {
            packet = modifier.modifyOutgoing(u, nm, packet);

            // Packet has been terminated
            if (packet == null)
              return;
          }

          // Run through all specific modifiers
          ArrayList<IPacketModifier> specifics = specificModifiers.get(u);
          if (specifics != null) {
            for (IPacketModifier modifier : specifics) {
              packet = modifier.modifyOutgoing(u, nm, packet);

              // Packet has been terminated
              if (packet == null)
                return;
            }
          }
        } catch (Exception e) {
          logger.logError(e);
        }

        // Relay modified packet
        super.write(ctx, packet, promise);
      }
    };

    // Packet handler registered already, add afterwards
    if (pipe.names().contains("packet_handler"))
      pipe.addBefore("packet_handler", handlerName, handler);

    // No packet handler yet, just register as last entry
    else
      pipe.addLast(handlerName, handler);
  }

  /**
   * Create a new injection for the player
   * @param p Target player, mandatory
   */
  private void injectPlayer(Player p) {
    refl.getNetworkChannel(p).ifPresent(nc -> {
      refl.getNetworkManager(p).ifPresent(nm -> {
        injectChannel(p, (NetworkManager) nm, nc.pipeline());
      });
    });
  }

  /**
   * Inject the local handler into all connections that still are missing it
   */
  private void injectAllConnections() {
    try {
      // Find all network managers inside the ServerConnection
      List<?> nmans = refl.getCraftServer()
        .flatMap(cs -> refl.getFieldByName(cs, "console"))
        .flatMap(console -> refl.getFieldByType(console, ServerConnection.class))
        .flatMap(sc ->
          refl.findListFieldByType(sc.getClass(), "NetworkManager")
            .flatMap(nmlf -> refl.getFieldValue(nmlf, sc))
        )
        .map(nml -> (List<?>) nml)
        .orElse(new ArrayList<>());

      // Call inject on all of them, as it will only register absent handlers
      for (Object nm : nmans) {
        refl.getNetworkChannel(nm).ifPresent(ch -> {
          injectChannel(null, (NetworkManager) nm, ch.pipeline());
        });
      }
    } catch (Exception e) {
      logger.logError(e);
    }
  }
}
