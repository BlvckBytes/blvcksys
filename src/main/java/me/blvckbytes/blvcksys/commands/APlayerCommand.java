package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.*;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

  // Arguments this command may be invoked with
  private final CommandArgument[] cmdArgs;

  // Mapping command names to their dispatchers
  private static final Map<String, APlayerCommand> registeredCommands;

  // Injected dependencies, leave them protected for quick access within command classes
  protected final JavaPlugin plugin;
  protected final ILogger logger;
  protected final IConfig cfg;
  protected final MCReflect refl;

  // The top level permission of this command
  private final PlayerPermission permission;

  // Mapping a player to their map of named cooldowns
  private final Map<Player, Map<String, Long>> playerCooldowns;

  static {
    registeredCommands = new HashMap<>();
  }

  /**
   * @param name Name of the command
   * @param description Description of the command
   * @param cmdArgs List of available arguments
   */
  public APlayerCommand(
    JavaPlugin plugin,
    ILogger logger,
    IConfig cfg,
    MCReflect refl,
    String name,
    String description,
    PlayerPermission permission,
    CommandArgument... cmdArgs
  ) {
    super(
      // Get the name from the first entry of the comma separated list
      name.split(",")[0],
      description,

      // Generate a usage string from all first tuple items of the args-map
      Arrays.stream(cmdArgs)
        .map(CommandArgument::getName)
        .reduce("/" + name, (acc, curr) -> acc + " " + curr),

      // Get aliases by the comma separated list "name"
      // Example: <main>,<alias 1>,<alias 2>
      Arrays.stream(name.split(",")).skip(1).map(String::trim).toList()
    );

    this.cmdArgs = cmdArgs;
    this.plugin = plugin;
    this.logger = logger;
    this.cfg = cfg;
    this.refl = refl;
    this.permission = permission;

    this.playerCooldowns = new HashMap<>();

    // Register this command within the server's command map
    refl.registerCommand(plugin.getDescription().getName(), this);
    registeredCommands.put(name, this);
    logger.logDebug("Command /" + name + ": " + this.getClass().getSimpleName());
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
    if (!(sender instanceof Player p))
      return new ArrayList<>();

    // Calculate the arg index
    int currArg = Math.max(0, args.length - 1);

    // Get it's connected permission
    PlayerPermission argPerm = cmdArgs[Math.min(currArg, cmdArgs.length - 1)].getPermission();

    // Doesn't have permission for this arg, don't invoke the completion callback
    if (argPerm != null && !argPerm.has(p))
      return new ArrayList<>();

    // Call tab completion handler and limit the results to 10 items
    return onTabCompletion(p, args, currArg)
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
      // Check for the top level permission
      if (permission != null && !permission.has(p))
        throw new MissingPermissionException(cfg, permission);

      // Check for all permissions regarding arguments
      for (int i = 0; i < args.length; i++) {
        PlayerPermission argPerm = cmdArgs[Math.max(i, cmdArgs.length - 1)].getPermission();
        if (argPerm != null && !argPerm.has(p))
          throw new MissingPermissionException(cfg, argPerm);
      }

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
  //                                Cooldowns                                //
  //=========================================================================//

  /**
   * Refresh a player's cooldown to start anew
   * @param p Target player
   * @param token Token that identifies this cooldown
   * @param duration Duration of this cooldown in milliseconds
   */
  protected void refreshCooldown(Player p, String token, long duration) {
    // Create empty cooldown map initially
    if (!this.playerCooldowns.containsKey(p))
      this.playerCooldowns.put(p, new HashMap<>());

    // Register the new cooldown
    this.playerCooldowns.get(p).put(token, System.currentTimeMillis() + duration);
  }

  /**
   * Clear a player's cooldown so it ends now
   * @param p Target player
   * @param token Token that identifies this cooldown
   */
  protected void clearCooldown(Player p, String token) {
    // Remove the entry if it exists
    if (this.playerCooldowns.containsKey(p))
      this.playerCooldowns.get(p).remove(token);
  }

  /**
   * Checks if a player's cooldown has expired already and throws
   * the matching parameterized exception if it hasn't
   * @param p Target player
   * @param token Token that identifies this cooldown
   */
  protected void cooldownGuard(Player p, String token) {
    // Player not even registered yet
    if (!this.playerCooldowns.containsKey(p))
      return;

    // Check if the timestamp is absent or expired
    Long expiry = this.playerCooldowns.get(p).get(token);
    if (expiry == null || System.currentTimeMillis() >= expiry)
      return;

    // Cooldown is active
    throw new CooldownException(cfg, expiry);
  }

  //=========================================================================//
  //                            Internal Utilities                           //
  //=========================================================================//

  /////////////////////////////// Suggestions //////////////////////////////////

  /**
   * Suggest an enum's values as autocompletion, used with {@link #onTabCompletion}
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param enumClass Class of the target enum
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
   * @param enumClass Class of the target enum
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
  protected Stream<String> suggestOnlinePlayers(String[] args, int currArg, Player... exclude) {
    return suggestOnlinePlayers(args, currArg, List.of(exclude));
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
   * Suggest all players that have ever played on this server
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @return Stream of suggestions
   */
  protected Stream<String> suggestOfflinePlayers(String[] args, int currArg) {
    return suggestOfflinePlayers(args, currArg, new ArrayList<>());
  }

  /**
   * Suggest all players that have ever played on this server, except the exclusion
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param exclude Players to exclude from the suggestion
   * @return Stream of suggestions
   */
  protected Stream<String> suggestOfflinePlayers(String[] args, int currArg, List<OfflinePlayer> exclude) {
    return Arrays.stream(Bukkit.getOfflinePlayers())
      .filter(p -> !exclude.contains(p))
      .filter(OfflinePlayer::hasPlayedBefore)
      .map(OfflinePlayer::getName)
      .filter(Objects::nonNull)
      .filter(n -> n.toLowerCase().contains(args[currArg].toLowerCase()));
  }

  ///////////////////////////////// Usage ////////////////////////////////////

  /**
   * Get an argument's placeholder by it's argument id (zero based index)
   * @param argId Argument id
   * @return Placeholder value
   */
  protected String getArgumentPlaceholder(int argId) {
    return cmdArgs[Math.min(argId, cmdArgs.length - 1)].getName();
  }

  /**
   * Build the usage-string in advanced mode, which supports hover tooltips
   * @param focusedArgument The argument that should be focused using the focus color
   */
  protected BaseComponent buildAdvancedUsage(@Nullable Integer focusedArgument) {
    BaseComponent head = new TextComponent();
    String cOth = cfg.get(ConfigKey.ERR_USAGE_COLOR_OTHER).asScalar();
    String cFoc = cfg.get(ConfigKey.ERR_USAGE_COLOR_FOCUS).asScalar();

    // Add /command with it's description as a tooltip
    head.addExtra(buildHoverable(cOth + "/" + getName(), cOth + getDescription()));

    // Add all it's arguments with their descriptive text as hover-tooltips
    for (int i = 0; i < this.cmdArgs.length; i++) {
      CommandArgument arg = this.cmdArgs[i];

      // Decide whether to colorize the argument using normal
      // colors or using the focus color based on it's positional index
      head.addExtra(buildHoverable(" " + colorizeUsage(arg.getName(), (focusedArgument != null && focusedArgument == i)), cOth + arg.getDescription()));
    }

    return head;
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

  ////////////////////////////// Custom Error ////////////////////////////////

  /**
   * Generate a custom error result
   * @param message Message to send to the player
   */
  protected void customError(String message) throws CommandException {
    throw new CommandException(message);
  }

  ///////////////////////////// Parsing: Player ///////////////////////////////

  /**
   * Get an online player by their name
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   */
  protected Player onlinePlayer(String[] args, int index) throws CommandException {
    return onlinePlayer(args, index, null);
  }

  /**
   * Get an online player by their name and provide a fallback
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   */
  protected Player onlinePlayer(String[] args, int index, @Nullable Player argcFallback) throws CommandException {
    // Index out of range
    if (index >= args.length) {
      // Fallback provided
      if (argcFallback != null)
        return argcFallback;

      // Focus arg
      throw new UsageMismatchException(cfg, buildAdvancedUsage(index));
    }

    Player target = Bukkit.getPlayerExact(args[index]);

    // The target player is not online at the moment
    if (target == null)
      throw new OfflineTargetException(cfg, args[index]);

    return target;
  }

  /**
   * Get a player that has played before by their name
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   */
  protected OfflinePlayer offlinePlayer(String[] args, int index) throws CommandException {
    // Index out of range
    if (index >= args.length) {
      // Focus arg
      throw new UsageMismatchException(cfg, buildAdvancedUsage(index));
    }

    // Find the first player that played before and has this name
    Optional<OfflinePlayer> res = Arrays.stream(Bukkit.getOfflinePlayers())
      .filter(OfflinePlayer::hasPlayedBefore)
      .filter(n -> n.getName() != null && n.getName().equals(args[index]))
      .findFirst();

    // That player has never played before
    if (res.isEmpty())
      throw new UnknownTargetException(cfg, args[index]);

    return res.get();
  }

  ///////////////////////////// Parsing: Integer ///////////////////////////////

  /**
   * Try to parse an integer value from a string and provide a fallback
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @return Parsed integer
   */
  protected int parseInt(String[] args, int index) throws CommandException {
    return parseInt(args, index, null);
  }

  /**
   * Try to parse an integer value from a string and provide a fallback
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   * @return Parsed integer
   */
  protected int parseInt(String[] args, int index, @Nullable Integer argcFallback) throws CommandException {
    if (index >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new UsageMismatchException(cfg, buildAdvancedUsage(index));
    }

    try {
      return Integer.parseInt(args[index]);
    } catch (NumberFormatException e) {
      throw new InvalidIntegerException(cfg, args[index]);
    }
  }

  ////////////////////////////// Parsing: Enum ////////////////////////////////

  /**
   * Parse an enum's value from a plain string (ignores casing)
   * @param enumClass Class of the target enum
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @return Parsed enum value
   */
  protected<T extends Enum<T>> T parseEnum(Class<T> enumClass, String[] args, int index) throws CommandException {
    return parseEnum(enumClass, args, index, null, new ArrayList<>());
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
    return parseEnum(enumClass, args, index, argcFallback, new ArrayList<>());
  }

  /**
   * Parse an enum's value from a plain string (ignores casing)
   * @param enumClass Class of the target enum
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param exclude Items to exclude from being valid
   * @return Parsed enum value
   */
  protected<T extends Enum<T>> T parseEnum(Class<T> enumClass, String[] args, int index, List<T> exclude) throws CommandException {
    return parseEnum(enumClass, args, index, null, exclude);
  }

  /**
   * Parse an enum's value from a plain string (ignores casing) and provide a fallback
   * in case the argument count isn't sufficient to fetch the required argument
   * @param enumClass Class of the target enum
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   * @param exclude Items to exclude from being valid
   * @return Parsed enum value
   */
  protected<T extends Enum<T>> T parseEnum(Class<T> enumClass, String[] args, int index, T argcFallback, List<T> exclude) throws CommandException {
    if (index >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new UsageMismatchException(cfg, buildAdvancedUsage(index));
    }

    // Find the enum constant by it's name
    for (T constant : enumClass.getEnumConstants()) {
      // This constant is excluded from being valid
      if (exclude.contains(constant))
        continue;

      if (constant.name().equalsIgnoreCase(args[index]))
        return constant;
    }

    // Could not find any matching constants
    throw new InvalidOptionException(cfg, args[index]);
  }

  /////////////////////////// Parsing: Argument spans /////////////////////////////

  /**
   * Collect a string that spans over multiple arguments
   * @param args Array of arguments
   * @param from Starting index
   * @param to Ending index
   * @return String containing space separated, joined arguments
   */
  protected String argspan(String[] args, int from, int to) throws CommandException {
    if (from >= args.length)
      throw new UsageMismatchException(cfg, buildAdvancedUsage(from));

    if (to >= args.length)
      throw new UsageMismatchException(cfg, buildAdvancedUsage(to));

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
  protected String argvar(String[] args, int from) throws CommandException {
    return argspan(args, from, args.length - 1);
  }

  /**
   * Get an argument's value by index
   * @param args Array of arguments
   * @param index Index of the target argumetn
   * @return Argument's value
   */
  protected String argval(String[] args, int index) {
    return argval(args, index, null);
  }

  /**
   * Get an argument's value by index
   * @param args Array of arguments
   * @param index Index of the target argumetn
   * @param fallback Fallback string in case the argument is missing
   * @return Argument's value
   */
  protected String argval(String[] args, int index, String fallback) {
    if (index >= args.length) {
      // Fallback provided
      if (fallback != null)
        return fallback;

      // Highlight missing arg
      throw new UsageMismatchException(cfg, buildAdvancedUsage(index));
    }

    return args[index];
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
    return cmdArgs[Math.min(argId, cmdArgs.length - 1)].getDescription();
  }

  /**
   * Colorize the usage string based on the colors specified inside the config
   * @param vanilla Vanilla usage string
   * @param focus Whether or not to use the focus color on arguments
   * @return Colorized usage string
   */
  public String colorizeUsage(String vanilla, boolean focus) {
    // Usage formatting colors
    String cMan = cfg.get(ConfigKey.ERR_USAGE_COLOR_MANDATORY).asScalar();
    String cOpt = cfg.get(ConfigKey.ERR_USAGE_COLOR_OPTIONAL).asScalar();
    String cBra = cfg.get(ConfigKey.ERR_USAGE_COLOR_BRACKETS).asScalar();
    String cOth = cfg.get(ConfigKey.ERR_USAGE_COLOR_OTHER).asScalar();

    // Focus arguments in focus mode
    if (focus)
      cMan = cOpt = cfg.get(ConfigKey.ERR_USAGE_COLOR_FOCUS).asScalar();

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
