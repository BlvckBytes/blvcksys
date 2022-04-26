package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.ObjectStringifier;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.cmd.CommandArgument;
import me.blvckbytes.blvcksys.util.cmd.exception.CommandException;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

@AutoConstruct
public class InjectCommand extends APlayerCommand implements Listener, IPacketModifier {

  /**
   * Describes the direction a intercepted packet can travel
   */
  private enum PacketDirection {
    IN,     // Inbound only
    OUT,    // Outbound only
    IN_OUT  // In- and outbound
  }

  /**
   * Describes an issued interception request and all it's parameters
   */
  private record InterceptionRequest(
    PacketDirection dir,  // Direction to intercept
    int depth,            // Max. recursion depth for stringification
    Pattern regex         // Pattern to match packet names against
  ) {}

  // Map assigning intercepting handlers to players
  private final Map<Player, InterceptionRequest> requests;

  private final IPacketInterceptor interceptor;
  private final ObjectStringifier ostr;

  public InjectCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPacketInterceptor interceptor,
    @AutoInject ObjectStringifier ostr
  ) {
    super(
      plugin, logger, cfg, refl,
      "inject",
      "Inject an interceptor to monitor a player's packets",
      PlayerPermission.COMMAND_INJECT,
      new CommandArgument("<player>", "Player to capture"),
      new CommandArgument("[direction]", "Direction of packets"),
      new CommandArgument("[depth]", "Recursion depth for stringification"),
      new CommandArgument("[regex]", "Regex pattern matching packet names")
    );

    this.ostr = ostr;
    this.interceptor = interceptor;
    this.requests = new HashMap<>();
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // First argument - provide all online players
    if (currArg == 0)
      return suggestOnlinePlayers(args, currArg);

    // Second argument - provide all PacketDirection values
    if (currArg == 1)
      return suggestEnum(args, currArg, PacketDirection.class);

    // Provide remaining args as placeholders (regex is variadic)
    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    // Get the target player
    Player target = onlinePlayer(args, 0);

    // The default direction is in and out
    PacketDirection dir = parseEnum(PacketDirection.class, args, 1, PacketDirection.IN_OUT);

    // Try to parse the depth, fallback to one if not provided
    int depth = parseInt(args, 2, 1);

    // The default regex is null (match everything)
    Pattern regex = null;

    // Regex has been provided, collect supporting spaces and try to compile the pattern
    if (args.length >= 4) {
      String regStr = argvar(args, 3);

      try {
        regex = Pattern.compile(regStr);
      } catch (PatternSyntaxException e) {
        customError(
          cfg.get(ConfigKey.INJECT_INVALID_REGEX)
            .withPrefix()
            .withVariable("regex", regStr)
            .asScalar()
        );
      }
    }

    // Already injected, uninject
    if (this.interceptor.isRegisteredSpecific(target, this)) {
      this.interceptor.unregisterSpecific(target, this);

      p.sendMessage(
        cfg.get(ConfigKey.INJECT_UNINJECTED)
          .withPrefix()
          .withVariable("target", target.getDisplayName())
          .asScalar()
      );

      return;
    }

    // Create a new injection and store the request locally
    this.requests.put(target, new InterceptionRequest(dir, depth, regex));
    this.interceptor.registerSpecific(target, this);

    p.sendMessage(
      cfg.get(ConfigKey.INJECT_INJECTED)
        .withPrefix()
        .withVariable("target", target.getDisplayName())
        .asScalar()
    );
  }

  //=========================================================================//
  //                                Modifiers                                //
  //=========================================================================//

  @Override
  public Packet<?> modifyIncoming(Player sender, NetworkManager nm, Packet<?> incoming) {
    // No request present yet
    InterceptionRequest req = requests.get(sender);
    if (req == null)
      return incoming;

    if (
      // Direction matches
      (req.dir == PacketDirection.IN || req.dir == PacketDirection.IN_OUT) &&

        // Pattern matches
        (req.regex == null || req.regex.matcher(incoming.getClass().getSimpleName()).find())
    )
      logEvent("INBOUND", incoming, req.depth);

    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(Player receiver, NetworkManager nm, Packet<?> outgoing) {
    // No request present yet
    InterceptionRequest req = requests.get(receiver);
    if (req == null)
      return outgoing;

    if (
      // Direction matches
      (req.dir == PacketDirection.OUT || req.dir == PacketDirection.IN_OUT) &&

        // Pattern matches
        (req.regex == null || req.regex.matcher(outgoing.getClass().getSimpleName()).find())
    )
      logEvent("OUTBOUND", outgoing, req.depth);

    return outgoing;
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Log an event of a passing packet
   * @param dir Direction the packet travels in
   * @param msg The packet
   * @param depth Levels of recursion to allow when stringifying object fields
   */
  private void logEvent(String dir, Object msg, int depth) {
    // Log this event as an info message
    logger.logInfo(
      cfg.get(ConfigKey.INJECT_EVENT)
        .withVariable("direction", dir)
        .withVariable("object", ostr.stringifyObject(msg, depth))
        .asScalar()
    );
  }
}
