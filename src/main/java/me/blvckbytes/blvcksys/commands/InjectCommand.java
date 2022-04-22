package me.blvckbytes.blvcksys.commands;

import io.netty.channel.*;
import me.blvckbytes.blvcksys.Main;
import me.blvckbytes.blvcksys.config.Config;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.cmd.CommandResult;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

@AutoConstruct
public class InjectCommand extends APlayerCommand implements Listener, IAutoConstructed {

  /**
   * Describes the direction a intercepted packet can travel
   */
  enum PacketDirection {
    IN,     // Inbound only
    OUT,    // Outbound only
    IN_OUT  // In- and outbound
  }

  // Name of the intercepting handler inside the channel pipeline
  private static final String handlerName = "blvcksys_injectcmd";

  // Map assigning intercepting handlers to players
  private final Map<Player, ChannelDuplexHandler> handlers;

  public InjectCommand() {
    super(
      "inject",
      "Inject an interceptor to monitor a player's packets",
      "/inject <player> [direction] [regex]"
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

    // Second argument - provide all PacketDirection values
    if (currArg == 1)
      return Arrays.stream(PacketDirection.values())
        .map(Enum::toString)
        .filter(pd -> pd.toLowerCase().contains(args[currArg].toLowerCase()));

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected CommandResult onInvocation(Player p, String label, String[] args) {
    if (args.length < 1)
      return usageMismatch();

    // Get the target player
    Player target = Bukkit.getPlayer(args[0]);
    if (target == null)
      return playerOffline(args[0]);

    // The default direction is in and out
    PacketDirection dir = PacketDirection.IN_OUT;

    // Try to parse the packet direction
    if (args.length >= 2) {
      String dirStr = args[1].toUpperCase();

      try {
        dir = PacketDirection.valueOf(dirStr);
      } catch (IllegalArgumentException e) {
        return customError(Config.getP(ConfigKey.INJECT_INVALID_DIR, dirStr));
      }
    }

    // The default regex is null (match everything)
    Pattern regex = null;

    // Regex has been provided, collect supporting spaces and try to compile the pattern
    if (args.length >= 3) {
      String regStr = argvar(args, 2);

      try {
        regex = Pattern.compile(regStr);
      } catch (PatternSyntaxException e) {
        return customError(Config.getP(ConfigKey.INJECT_INVALID_REGEX, regStr));
      }
    }

    // Already injected, uninject
    if (this.handlers.containsKey(target)) {
      if (this.uninjectPlayer(target))
        p.sendMessage(Config.getP(ConfigKey.INJECT_UNINJECTED, target.getDisplayName()));
      else
        p.sendMessage(Config.getP(ConfigKey.ERR_INTERNAL));
      return success();
    }

    // Create a new injection
    if (this.injectPlayer(target, dir, regex))
      p.sendMessage(Config.getP(ConfigKey.INJECT_INJECTED, target.getDisplayName()));
    else
      p.sendMessage(Config.getP(ConfigKey.ERR_INTERNAL));
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
      Class<?> cl = msg.getClass();
      Field[] fields = cl.getDeclaredFields();

      // This class doesn't contain any fields, search for superclasses
      while (
        // No fields yet
        fields.length == 0 &&

        // Superclass available
        msg.getClass().getSuperclass() != null
      ) {
        // Navigate into superclass and list it's fields
        cl = cl.getSuperclass();
        fields = cl.getDeclaredFields();
      }

      // Loop all fields of this packet and add them to a comma separated list
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
   * @param dir Direction to capture
   * @param regex Regex to match simple classnames against, null for any
   * @return Success state
   */
  private boolean injectPlayer(Player p, PacketDirection dir, Pattern regex) {
    try {
      // Already injected
      if (handlers.containsKey(p))
        return true;

      // Create a new channel handler that overrides R/W to intercept
      // This handler gets created in this closure to provide player context (if ever needed)
      ChannelDuplexHandler handler = new ChannelDuplexHandler() {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
          if (
            // Direction matches
            (dir == PacketDirection.IN || dir == PacketDirection.IN_OUT) &&

            // Pattern matches
            (regex == null || regex.matcher(msg.getClass().getSimpleName()).find())
          )
            logEvent("INBOUND", msg);

          super.channelRead(ctx, msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
          if (
            // Direction matches
            (dir == PacketDirection.OUT || dir == PacketDirection.IN_OUT) &&

            // Pattern matches
            (regex == null || regex.matcher(msg.getClass().getSimpleName()).find())
          )
            logEvent("OUTBOUND", msg);

          super.write(ctx, msg, promise);
        }
      };

      // Register handler in local map
      handlers.put(p, handler);

      // Add custom interception handler before default packet handler
      getPipe(p).addBefore("packet_handler", handlerName, handler);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

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
   * @return Success state
   */
  private boolean uninjectPlayer(Player p) {
    try {
      // Not injected
      if (handlers.remove(p) == null)
        return true;

      // Remove pipeline entry
      ChannelPipeline pipe = getPipe(p);

      // Not registered in the pipeline
      if (!pipe.names().contains(handlerName))
        return true;

      // Remove handler
      pipe.remove(handlerName);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
