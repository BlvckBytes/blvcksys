package me.blvckbytes.blvcksys.util.cmd;

import com.google.common.collect.Lists;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.cmd.exception.*;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
   * @throws CommandException An exception during the invocation of this command
   */
  protected abstract void invoke(Player p, String label, String[] args) throws CommandException;

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
  @NotNull
  public List<String> tabComplete(
    @NotNull CommandSender sender,
    @NotNull String alias,
    @NotNull String[] args
  ) throws IllegalArgumentException {
    // Don't serve non-players
    if (!(sender instanceof Player))
      return new ArrayList<>();

    // Call tab completion handler and limit the results to 10 items
    return onTabCompletion((Player) sender, args, Math.max(0, args.length - 1))
      .limit(10)
      .toList();
  }

  @Override
  public boolean execute(
    @NotNull CommandSender cs,
    @NotNull String label,
    @NotNull String[] args
  ) {
    // Not a player
    if (!(cs instanceof Player p)) {
      cs.sendMessage(cfg.get(ConfigKey.ERR_NOT_A_PLAYER).asScalar());
      return false;
    }

    try {
      // Relay handling
      invoke(p, label, args);
      return true;
    } catch (CommandException e) {
      // Send the exceptions text to the executor
      p.spigot().sendMessage(e.getText());
      return false;
    }
  }

  //=========================================================================//
  //                            Internal Utilities                           //
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
   * Suggest an enum's values as autocompletion, used with {@link #onTabCompletion}
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param exclude Enum values to exclude
   * @return Stream of suggestions
   */
  @SafeVarargs
  protected final <T extends Enum<T>> Stream<String> suggestEnum(String[] args, int currArg, Class<T> enumClass, T... exclude) {
    return suggestEnum(args, currArg, enumClass, List.of(exclude));
  }

  /**
   * Suggest an enum's values as autocompletion, used with {@link #onTabCompletion}
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param exclude Enum values to exclude
   * @return Stream of suggestions
   */
  protected<T extends Enum<T>> Stream<String> suggestEnum(String[] args, int currArg, Class<T> enumClass, List<T> exclude) {
    return Arrays.stream(enumClass.getEnumConstants())
      .filter(c -> !exclude.contains(c))
      .map(Enum::toString)
      .filter(m -> m.toLowerCase().contains(args[currArg].toLowerCase()));
  }

  /**
   * Suggest all currently online players, except the exclusion
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param exclude Players to exclude from the suggestion
   * @return Stream of suggestions
   */
  protected Stream<String> suggestOnlinePlayers(String[] args, int currArg, List<Player> exclude) {
    return Bukkit.getOnlinePlayers()
      .stream()
      .filter(n -> !exclude.contains(n))
      .map(Player::getDisplayName)
      .filter(n -> n.toLowerCase().contains(args[currArg].toLowerCase()));
  }

  /**
   * Suggest all currently online players, except the exclusion
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param exclude Players to exclude from the suggestion
   * @return Stream of suggestions
   */
  protected Stream<String> suggestOnlinePlayers(String[] args, int currArg, Player... exclude) {
    return suggestOnlinePlayers(args, currArg, List.of(exclude));
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
   */
  protected BaseComponent buildAdvancedUsage() {
    BaseComponent head = new TextComponent();
    String cOth = cfg.get(ConfigKey.ERR_USAGE_COLOR_OTHER).asScalar();

    // Add /command with it's description as a tooltip
    head.addExtra(buildHoverable(cOth + "/" + getName(), cOth + getDescription()));

    // Add all it's arguments with their descriptive text as hover-tooltips
    for (String[] desc : this.argDescs)
      head.addExtra(buildHoverable(" " + colorizeUsage(desc[0]), cOth + desc[1]));

    return head;
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
   * Generate a usage-mismatch result
   */
  protected void usageMismatch() throws CommandException {
    throw new UsageMismatchException(cfg, buildAdvancedUsage());
  }

  /**
   * Get an online player by their name
   * @param name Name of the online player
   */
  protected Player onlinePlayer(String name) throws CommandException {
    Player target = Bukkit.getPlayerExact(name);

    // The target player is not online at the moment
    if (target == null)
      throw new OfflineTargetException(cfg, name);

    return target;
  }

  /**
   * Generate a custom error result
   * @param message Message to send to the player
   */
  protected void customError(String message) throws CommandException {
    throw new CommandException(message);
  }

  /**
   * Try to parse an integer value from a string
   * @param value String to parse
   * @return Parsed integer
   */
  protected int parseInt(String value) throws CommandException {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      throw new InvalidIntegerException(cfg, value);
    }
  }

  /**
   * Parse an enum's value from a plain string (ignores casing)
   * @param enumClass Class of the target enum
   * @param value String to parse
   * @return Parsed enum value
   */
  protected<T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) throws CommandException {
    return parseEnum(enumClass, value, true);
  }

  /**
   * Parse an enum's value from a plain string
   * @param enumClass Class of the target enum
   * @param value String to parse
   * @param ignoreCase Whether or not to ignore casing
   * @return Parsed enum value
   */
  protected<T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, boolean ignoreCase) throws CommandException {
    return parseEnum(enumClass, value, ignoreCase, new ArrayList<>());
  }

  /**
   * Parse an enum's value from a plain string
   * @param enumClass Class of the target enum
   * @param value String to parse
   * @param ignoreCase Whether or not to ignore casing
   * @param exclude Items to exclude from being valid
   * @return Parsed enum value
   */
  protected<T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, boolean ignoreCase, List<T> exclude) throws CommandException {
    // Find the enum constant by it's name
    for (T constant : enumClass.getEnumConstants()) {
      // This constant is excluded from being valid
      if (exclude.contains(constant))
        continue;

      // Parse with ignore casing as requested by the flag
      if (ignoreCase && constant.name().equalsIgnoreCase(value) || constant.name().equals(value))
        return constant;
    }

    // Could not find any matching constants
    throw new InvalidOptionException(cfg, value);
  }

  /**
   * Parse an enum's value from a plain string (ignores casing) and provide a fallback
   * in case the argument count isn't sufficient to fetch the required argument
   * @param enumClass Class of the target enum
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   * @return Parsed enum value
   */
  protected<T extends Enum<T>> T parseEnum(Class<T> enumClass, String[] args, int index, T argcFallback) throws CommandException {
    return parseEnum(enumClass, args, index, true, argcFallback, new ArrayList<>());
  }

  /**
   * Parse an enum's value from a plain string and provide a fallback
   * in case the argument count isn't sufficient to fetch the required argument
   * @param enumClass Class of the target enum
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param ignoreCase Whether or not to ignore casing
   * @param argcFallback Fallback value to use
   * @param exclude Items to exclude from being valid
   * @return Parsed enum value
   */
  protected<T extends Enum<T>> T parseEnum(Class<T> enumClass, String[] args, int index, boolean ignoreCase, T argcFallback, List<T> exclude) throws CommandException {
    // Index out of range, provide fallback
    if (index >= args.length)
      return argcFallback;

    // Relay parsing
    return parseEnum(enumClass, args[index], ignoreCase, exclude);
  }

  //=========================================================================//
  //                             Public Utilities                            //
  //=========================================================================//

  /**
   * Get an argument's description by it's argument id (zero based index)
   * @param argId Argument id
   * @return Description value
   */
  public String getArgumentDescripton(int argId) {
    return argDescs[Math.min(argId, argDescs.length - 1)][1];
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

  //=========================================================================//
  //                             Static Utilities                            //
  //=========================================================================//

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
