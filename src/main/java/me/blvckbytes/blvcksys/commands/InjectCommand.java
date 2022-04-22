package me.blvckbytes.blvcksys.commands;

import io.netty.channel.*;
import me.blvckbytes.blvcksys.Main;
import me.blvckbytes.blvcksys.config.Config;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.cmd.CommandResult;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.network.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@AutoConstruct
public class InjectCommand extends APlayerCommand implements Listener, IAutoConstructed {

  // Name of the intercepting handler inside the channel pipeline
  private static final String handlerName = "blvcksys_injectcmd";

  // Map assigning intercepting handlers to players
  private final Map<Player, ChannelDuplexHandler> handlers;

  public InjectCommand() {
    super(
      "inject",
      "Inject an interceptor to monitor a player's packets",
      "/inject <player>"
    );

    this.handlers = new HashMap<>();
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // First argument - provide all online players
    if (currArg == 0)
      return Bukkit.getOnlinePlayers()
        .stream()
        .map(Player::getDisplayName)
        .filter(n -> n.toLowerCase().contains(args[currArg].toLowerCase()));

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected CommandResult onInvocation(Player p, String label, String[] args) {
    if (args.length != 1)
      return usageMismatch();

    // Get the target player
    Player target = Bukkit.getPlayer(args[0]);
    if (target == null)
      return playerOffline(args[0]);

    // Already injected, uninject
    if (this.handlers.containsKey(target)) {
      this.uninjectPlayer(target);
      p.sendMessage(Config.getP(ConfigKey.INJECT_UNINJECTED, target.getDisplayName()));
      return success();
    }

    // Create a new injection
    this.injectPlayer(target);
    p.sendMessage(Config.getP(ConfigKey.INJECT_INJECTED, target.getDisplayName()));
    return success();
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Uninject players before they quit
    uninjectPlayer(e.getPlayer());
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void cleanup() {
    // Uninject all players
    for (Player p : this.handlers.keySet())
      uninjectPlayer(p);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Log an event of a passing packet
   * @param dir Direction the packet travels in
   * @param msg The packet
   */
  private void logEvent(String dir, Object msg) {
    StringBuilder props = new StringBuilder();

    try {
      // Loop all fields of this packet and add them to a comma separated list
      Field[] fields = msg.getClass().getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        Field f = fields[i];

        // Also access private fields, of course
        f.setAccessible(true);

        // Stringify and append with leading comma, if applicable
        props.append(i == 0 ? "" : ", ").append(stringifyObject(f.get(msg)));
      }
    } catch (Exception e) {
      Main.logger().logError(e);
    }

    // Log this event as an info message
    Main.logger().logInfo(Config.get(
      ConfigKey.INJECT_EVENT,
      dir, msg.getClass().getSimpleName(), props.toString()
    ));
  }

  /**
   * Create a new injection for the player
   * @param p Target player
   */
  private void injectPlayer(Player p) {
    // Already injected
    if (handlers.containsKey(p))
      return;

    // Create a new channel handler that overrides R/W to intercept
    // This handler gets created in this closure to provide player context (if ever needed)
    ChannelDuplexHandler handler = new ChannelDuplexHandler() {

      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logEvent("INBOUND", msg);
        super.channelRead(ctx, msg);
      }

      @Override
      public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        logEvent("OUTBOUND", msg);
        super.write(ctx, msg, promise);
      }
    };

    // Register handler in local map
    handlers.put(p, handler);

    // Add custom interception handler before default packet handler
    getPipe(p).addBefore("packet_handler", handlerName, handler);
  }

  private ChannelPipeline getPipe(Player p) {
    CraftPlayer cp = ((CraftPlayer) p);
    PlayerConnection pc = cp.getHandle().b;
    NetworkManager nm = pc.a;
    Channel c = nm.m;
    return c.pipeline();
  }

  /**
   * Remove a previously created injection from the player
   * @param p Target player
   */
  private void uninjectPlayer(Player p) {
    // Not injected
    if (handlers.remove(p) == null)
      return;

    // Remove pipeline entry
    ChannelPipeline pipe = getPipe(p);

    // Not registered in the pipeline
    if (!pipe.names().contains(handlerName))
      return;

    // Remove handler
    pipe.remove(handlerName);
  }
}
