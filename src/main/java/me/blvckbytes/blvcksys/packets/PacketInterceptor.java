package me.blvckbytes.blvcksys.packets;

import io.netty.channel.*;
import me.blvckbytes.blvcksys.Main;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import net.minecraft.network.protocol.Packet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoConstruct
public class PacketInterceptor implements IPacketInterceptor, Listener, IAutoConstructed {

  private static final String handlerName = "packet_interceptor";
  private final List<IPacketModifier> globalModifiers;
  private final Map<Player, ArrayList<IPacketModifier>> specificModifiers;

  public PacketInterceptor() {
    this.globalModifiers = new ArrayList<>();
    this.specificModifiers = new HashMap<>();
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
  public void registerSpecific(Player target, IPacketModifier modifier) {
    // Create empty list to add to
    if (!this.specificModifiers.containsKey(target))
      this.specificModifiers.put(target, new ArrayList<>());

    // Add modifier to list
    this.specificModifiers.get(target).add(modifier);
  }

  @Override
  public void unregisterSpecific(Player target, IPacketModifier modifier) {
    // Player not even known yet
    if (!this.specificModifiers.containsKey(target))
      return;

    // Remove modifier from list
    List<IPacketModifier> modifiers = this.specificModifiers.get(target);
    modifiers.remove(modifier);

    // Remove from map when no more modifiers remain
    if (modifiers.size() == 0)
      this.specificModifiers.remove(target);
  }

  @Override
  public boolean isRegisteredSpecific(Player target, IPacketModifier modifier) {
    return this.specificModifiers.getOrDefault(target, new ArrayList<>()).contains(modifier);
  }

  @Override
  public void cleanup() {
    // Unregister all globals
    for (int i = this.globalModifiers.size() - 1; i >= 0; i--)
      this.unregister(this.globalModifiers.get(i));

    // Unregister all specifics
    for (Map.Entry<Player, ArrayList<IPacketModifier>> entry : specificModifiers.entrySet())
      for (IPacketModifier modifier : entry.getValue())
        this.unregisterSpecific(entry.getKey(), modifier);

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

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    injectPlayer(e.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    uninjectPlayer(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Get the network pipeline of a player
   * @param p Target player
   * @return Pipeline of the target
   * @throws Exception Issues during reflection access
   */
  private ChannelPipeline getPipe(Player p) throws Exception {
    Channel c = MCReflect.getNetworkChannel(p);
    return c.pipeline();
  }

  /**
   * Remove a previously created injection from the player
   * @param p Target player
   */
  private void uninjectPlayer(Player p) {
    try {
      // Remove pipeline entry
      ChannelPipeline pipe = getPipe(p);

      // Not registered in the pipeline
      if (!pipe.names().contains(handlerName))
        return;

      // Remove handler
      pipe.remove(handlerName);
    } catch (Exception e) {
      Main.logger().logError(e);
    }
  }

  /**
   * Create a new injection for the player
   * @param p Target player
   */
  private void injectPlayer(Player p) {
    try {
      // Already registered in the pipeline
      ChannelPipeline pipe = getPipe(p);
      if (pipe.names().contains(handlerName))
        return;

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

          // Run through all global modifiers
          for (IPacketModifier modifier : globalModifiers)
            packet = modifier.modifyIncoming(p, packet);

          // Run through all specific modifiers
          ArrayList<IPacketModifier> specifics = specificModifiers.get(p);
          if (specifics != null) {
            for (IPacketModifier modifier : specifics)
              packet = modifier.modifyIncoming(p, packet);
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

          // Run through all global modifiers
          for (IPacketModifier modifier : globalModifiers)
            packet = modifier.modifyOutgoing(p, packet);

          // Run through all specific modifiers
          ArrayList<IPacketModifier> specifics = specificModifiers.get(p);
          if (specifics != null) {
            for (IPacketModifier modifier : specifics)
              packet = modifier.modifyOutgoing(p, packet);
          }

          // Relay modified packet
          super.write(ctx, packet, promise);
        }
      };

      pipe.addBefore("packet_handler", handlerName, handler);
    } catch (Exception e) {
      Main.logger().logError(e);
    }
  }
}
