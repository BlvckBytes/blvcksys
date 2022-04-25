package me.blvckbytes.blvcksys.util.cmd;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

/**
 * Represents the base of every command which is only available for players
 *
 * - Non-players are informed
 * - Commands get registered automatically
 * - Tab completion with internal result-limiting
 * - Execution results are handled properly
 * - Many useful utility methods
 */
public abstract class APlayerCommand extends Command {

  // Argument placeholder to description map
  private final String[][] argDescs;

  // Mapping command names to their dispatchers
  private static final Map<String, APlayerCommand> registeredCommands;

  // Injected dependencies, leave them protected for quick access within command classes
  protected final JavaPlugin plugin;
  protected final ILogger logger;
  protected final IConfig cfg;
  protected final MCReflect refl;

  static {
    registeredCommands = new HashMap<>();
  }

  /**
   * @param name Name of the command
   * @param description Description of the command
   * @param argDescs Mapping
   * @param aliases Aliases the command can also be called by
   */
  public APlayerCommand(
    JavaPlugin plugin,
    ILogger logger,
    IConfig cfg,
    MCReflect refl,
    String name,
    String description,
    String[][] argDescs,
    String ...aliases
  ) {
    super(
      name,
      description,

      // Generate a usage string from all first tuple items of the args-map
      Arrays.stream(argDescs)
        .map(strings -> strings[0])
        .reduce("/" + name, (acc, curr) -> acc + " " + curr),

      Arrays.asList(aliases)
    );

    this.argDescs = argDescs;
    this.plugin = plugin;
    this.logger = logger;
    this.cfg = cfg;
    this.refl = refl;

    // Register this command within the server's command map
    refl.registerCommand(plugin.getDescription().getName(), this);
    registeredCommands.put(name, this);
    logger.logDebug("Registered command /%s using handler %s", name, this.getClass().getName());
  }


  //=========================================================================//
  //                              Overrideables                              //
  //=========================================================================//

  /**
   * Callback method for command invocations
   * @param p Executing player
   * @param label Label of the command, either name or an alias
   * @param args Args passed with the command
   */
  protected abstract CommandResult onInvocation(Player p, String label, String[] args);

  /**
   * Callback method for command autocompletion (tab)
   * @param p Executing player
   * @param args Existing arguments in the chat-bar
   * @param currArg Index of the current argument within args
   * @return Stream of suggestions, will limited to 10 internally
   */
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    return Stream.empty();
  }

  //=========================================================================//
  //                                 Command                                 //
  //=========================================================================//

  @Override
  public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
    // Don't serve non-players
    if (!(sender instanceof Player))
      return new ArrayList<>();

    // Call tab completion handler and limit the results to 10 items
    return onTabCompletion((Player) sender, args, Math.max(0, args.length - 1))
      .limit(10)
      .toList();
  }

  @Override
  public boolean execute(CommandSender cs, String label, String[] args) {
    // Not a player
    if (!(cs instanceof Player)) {
      cs.sendMessage(cfg.get(ConfigKey.ERR_NOT_A_PLAYER).asScalar());
      return false;
    }

    // Relay handling, send the usage if exec was not successful
    CommandResult res = onInvocation((Player) cs, label, args);

    // No error occurred
    if (res.error() == CommandError.NONE)
      return true;

    // Decide on error-response
    switch (res.error()) {
      case PLAYER_NOT_ONLINE -> cs.sendMessage(
        cfg.get(ConfigKey.ERR_NOT_ONLINE)
          .withPrefix()
          .withVariable("player", res.text())
          .asScalar()
      );

      case USAGE_MISMATCH -> cs.spigot().sendMessage(
        buildAdvancedUsage(
          cfg.get(ConfigKey.ERR_USAGE)
            .withPrefix()
            .asScalar()
        )
      );

      // Custom error string, send as is
      case CUSTOM_ERROR -> cs.sendMessage(res.text());

      // Unparsable integer
      case INT_UNPARSEABLE -> cs.sendMessage(
        cfg.get(ConfigKey.ERR_INTPARSE)
          .withPrefix()
          .withVariable("number", res.text())
          .asScalar()
      );

      default -> cs.sendMessage(
        cfg.get(ConfigKey.ERR_INTERNAL)
          .withPrefix()
          .asScalar()
      );
    }

    return false;
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Get an argument's placeholder by it's argument id (zero based index)
   * @param argId Argument id
   * @return Placeholder value
   */
  protected String getArgumentPlaceholder(int argId) {
    return argDescs[Math.min(argId, argDescs.length - 1)][0];
  }

  /**
   * Get an argument's description by it's argument id (zero based index)
   * @param argId Argument id
   * @return Description value
   */
  public String getArgumentDescripton(int argId) {
    return argDescs[Math.min(argId, argDescs.length - 1)][1];
  }

  /**
   * Build a hoverable message
   * @param text Message to send
   * @param hover Message to display on hover
   * @return Built component
   */
  private TextComponent buildHoverable(String text, String hover) {
    // Build the hoverable text-component and add it to the list
    TextComponent tc = new TextComponent(text);
    tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
    return tc;
  }

  /**
   * Build the usage-string in advanced mode, which supports hover tooltips
   * @param prefix Prepended text, can be null if not needed
   */
  protected TextComponent[] buildAdvancedUsage(String prefix) {
    List<TextComponent> components = new ArrayList<>();
    String cOth = cfg.get(ConfigKey.ERR_USAGE_COLOR_OTHER).asScalar();

    // Add a prefix, if provided
    if (prefix != null)
      components.add(new TextComponent(prefix));

    // Add /command with it's description as a tooltip
    components.add(buildHoverable(cOth + "/" + getName(), cOth + getDescription()));

    // Add all it's arguments with their descriptive text as hover-tooltips
    for (String[] desc : this.argDescs)
      components.add(buildHoverable(" " + colorizeUsage(desc[0]), cOth + desc[1]));

    return components.toArray(TextComponent[]::new);
  }

  /**
   * Colorize the usage string based on the colors specified inside the config
   * @param vanilla Vanilla usage string
   * @return Colorized usage string
   */
  public String colorizeUsage(String vanilla) {
    // Usage formatting colors
    String cMan = cfg.get(ConfigKey.ERR_USAGE_COLOR_MANDATORY).asScalar();
    String cOpt = cfg.get(ConfigKey.ERR_USAGE_COLOR_OPTIONAL).asScalar();
    String cBra = cfg.get(ConfigKey.ERR_USAGE_COLOR_BRACKETS).asScalar();
    String cOth = cfg.get(ConfigKey.ERR_USAGE_COLOR_OTHER).asScalar();

    // Start out by coloring other
    StringBuilder colorized = new StringBuilder(cOth);

    /*
      - <...> = ... Mandatory
      - [...] = ... is Optional
      - <>[]  = Brackets
      - remaining = other
     */

    // Loop individual chars
    for (char c : vanilla.toCharArray()) {
      // Colorize bracket
      if (c == '<' || c == '>' || c == '[' || c == ']') {
        colorized.append(cBra);
        colorized.append(c);
      }

      // Begin of mandatory
      if (c == '<') {
        colorized.append(cMan);
      }

      // Begin of optional
      else if (c == '[') {
        colorized.append(cOpt);
      }

      // End of brackets, go back to other color
      else if (c == '>' || c == ']') {
        colorized.append(cOth);
      }

      // No bracket, normal text chars
      else
        colorized.append(c);
    }

    return colorized.toString();
  }

  /**
   * Collect a string that spans over multiple arguments
   * @param args Array of arguments
   * @param from Starting index
   * @param to Ending index
   * @return String containing space separated, joined arguments
   */
  protected String argspan(String[] args, int from, int to) {
    StringBuilder message = new StringBuilder();

    // Loop from - to (including), append spaces to separate as needed
    for (int i = from; i <= to; i++)
      message.append(i == from ? "" : " ").append(args[i]);

    return message.toString();
  }

  /**
   * Collect a string that spans over multiple arguments till the end
   * @param args Array of arguments
   * @param from Starting index
   * @return String containing space separated, joined arguments
   */
  protected String argvar(String[] args, int from) {
    return argspan(args, from, args.length - 1);
  }

  /**
   * Generate a success result
   */
  protected CommandResult success() {
    return new CommandResult(CommandError.NONE, null);
  }

  /**
   * Generate a usage-mismatch result
   */
  protected CommandResult usageMismatch() {
    return new CommandResult(CommandError.USAGE_MISMATCH, null);
  }

  /**
   * Generate a player-is-offline result
   */
  protected CommandResult playerOffline(String name) {
    return new CommandResult(CommandError.PLAYER_NOT_ONLINE, name);
  }

  /**
   * Generate a custom error result
   */
  protected CommandResult customError(String message) {
    return new CommandResult(CommandError.CUSTOM_ERROR, message);
  }

  /**
   * Try to parse an integer value from a string
   * @param value String to parse
   * @param out Output buffer
   * @return CommandResult containing the error or null on success
   */
  protected CommandResult parseInt(String value, MutableInt out) {
    try {
      out.setValue(Integer.parseInt(value));
      return null;
    } catch (Exception e) {
      return new CommandResult(CommandError.INT_UNPARSEABLE, value);
    }
  }

  /**
   * Get a command by it's command name string
   * @param command Command name string, casing will be ignored
   */
  public static Optional<APlayerCommand> getByCommand(String command) {
    for (Map.Entry<String, APlayerCommand> entry : registeredCommands.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(command))
        return Optional.of(entry.getValue());
    }

    return Optional.empty();
  }
}
