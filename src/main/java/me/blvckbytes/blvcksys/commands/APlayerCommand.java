package me.blvckbytes.blvcksys.commands;

import lombok.Getter;
import me.blvckbytes.blvcksys.commands.exceptions.*;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.ACooldownModel;
import me.blvckbytes.blvcksys.persistence.models.APersistentModel;
import me.blvckbytes.blvcksys.persistence.models.CooldownSessionModel;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.util.TimeUtil;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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

  // Used to remove vanished players from suggestions for non-bypassing players
  @AutoInjectLate
  private IVanishCommand vanish;

  @AutoInjectLate
  private TimeUtil timeUtil;

  // The top level permission of this command
  @Getter
  private final PlayerPermission rootPerm;

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
    @Nullable PlayerPermission rootPerm,
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

    // Set the command's permission to disallow command completion for
    // commands the player has no permission to execute
    if (rootPerm != null)
      setPermission(rootPerm.getValue());

    this.cmdArgs = cmdArgs;
    this.plugin = plugin;
    this.logger = logger;
    this.cfg = cfg;
    this.refl = refl;
    this.rootPerm = rootPerm;

    // Register this command within the server's command map
    try {
      refl.registerCommand(plugin.getDescription().getName(), this);
      registeredCommands.put(name, this);
      logger.logDebug("Command /" + name + ": " + this.getClass().getSimpleName());
    } catch (Exception e) {
      logger.logError(e);
    }
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

    if (cmdArgs.length == 0)
      return new ArrayList<>();

    // Calculate the arg index
    int currArg = Math.max(0, args.length - 1);

    // Doesn't have permission to invoke this command
    if (rootPerm != null && !rootPerm.has(p))
      return new ArrayList<>();

    // Get it's connected permission
    PlayerPermission argPerm = cmdArgs[Math.min(currArg, cmdArgs.length - 1)].getPermission();

    // Doesn't have permission for this arg
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
      if (rootPerm != null && !rootPerm.has(p))
        throw new MissingPermissionException(cfg, rootPerm);

      // Check for all permissions regarding arguments
      for (int i = 0; i < args.length; i++) {
        PlayerPermission argPerm = cmdArgs[Math.min(i, cmdArgs.length - 1)].getPermission();
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
    } catch (Exception e) {
      logger.logError(e);
      p.sendMessage(
        cfg.get(ConfigKey.ERR_INTERNAL)
          .withPrefix()
          .asScalar()
      );
      return false;
    }
  }

  //=========================================================================//
  //                                Cooldowns                                //
  //=========================================================================//

  /**
   * Protect a section of code with a cooldown and allow for a
   * bypass by providing a permission
   * @param p Target player
   * @param pers Persistence ref
   * @param model Model to create the cooldown for
   * @param message Message to display
   */
  protected void cooldownGuard(
    Player p,
    IPersistence pers,
    ACooldownModel model,
    ConfigValue message
  ) throws CooldownException {
    long cooldown = model.getCooldownRemaining(p, pers);

    if (cooldown > 0) {
      throw new CooldownException(
        message,
        timeUtil == null ? "?" : timeUtil.formatDuration(cooldown)
      );
    }

    model.storeCooldownFor(p, pers);
  }

  /**
   * Protect a section of code with a cooldown
   * @param p Target player
   * @param pers Persistence ref
   * @param token Unique token representing this command
   * @param duration Duration in seconds
   * @param bypassPermission Permission that allows the user to bypass this cooldown
   */
  protected void cooldownGuard(
    Player p,
    IPersistence pers,
    String token,
    int duration,
    PlayerPermission bypassPermission
  ) throws CooldownException {
    if (bypassPermission.has(p))
      return;

    long cooldown = CooldownSessionModel.getCooldownRemaining(p, pers, token);

    if (cooldown > 0) {
      throw new CooldownException(
        cfg.get(ConfigKey.ERR_COOLDOWN)
          .withPrefix(),
        timeUtil == null ? "?" : timeUtil.formatDuration(cooldown)
      );
    }

    pers.store(new CooldownSessionModel(p, duration, token));
  }

  //=========================================================================//
  //                            Internal Utilities                           //
  //=========================================================================//

  /////////////////////////////// Suggestions //////////////////////////////////

  /**
   * Create a stream of suggestions based on a list of models who's named
   * field contains the currently typed out argument, ignoring casing.
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param model Model to search for
   * @param field Target field of the model
   * @param pers Persistence ref
   * @return Stream of suggestions
   */
  protected Stream<String> suggestModels(
    String[] args,
    int currArg,
    Class<? extends APersistentModel> model,
    String field,
    IPersistence pers
  ) {
    return pers.findRaw(
        new QueryBuilder<>(
          model,
          field, EqualityOperation.CONT_IC, args[currArg]
        )
          .limit(10), field
      )
      .stream()
      .map(m -> m.get(field))
      .filter(Objects::nonNull)
      .map(Objects::toString);
  }

  /**
   * Suggest an enum's values as autocompletion, used with {@link #onTabCompletion}
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param enumClass Class of the target enum
   * @return Stream of suggestions
   */
  protected<T extends Enum<T>> Stream<String> suggestEnum(
    String[] args,
    int currArg,
    Class<T> enumClass
  ) {
    return suggestEnum(args, currArg, enumClass, (acc, curr) -> acc.add(curr.name()));
  }

  /**
   * Suggest an enum's values as autocompletion by using a custom reducer, used with {@link #onTabCompletion}
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param enumClass Class of the target enum
   * @param reducer Reducing function (acc, curr)
   * @return Stream of suggestions
   */
  protected<T extends Enum<T>> Stream<String> suggestEnum(
    String[] args,
    int currArg,
    Class<T> enumClass,
    BiConsumer<List<String>, T> reducer
  ) {
    // Collect all enum values through the reducer
    List<String> suggestions = new ArrayList<>();
    for (T c : enumClass.getEnumConstants())
      reducer.accept(suggestions, c);

    // Filter and sort the reducer's resutls
    return suggestions
      .stream()
      .sorted()
      .filter(m -> m.toLowerCase().contains(args[currArg].toLowerCase()));
  }

  /**
   * Suggest all currently online players, except the exclusion
   * @param p Invoking player
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param suggestAll Whether to suggest "all" as an option
   * @param exclude Players to exclude from the suggestion
   * @return Stream of suggestions
   */
  protected Stream<String> suggestOnlinePlayers(Player p, String[] args, int currArg, boolean suggestAll, Player... exclude) {
    return suggestOnlinePlayers(p, args, currArg, suggestAll, List.of(exclude));
  }

  /**
   * Suggest all currently online players, except the exclusion
   * @param p Invoking player
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param suggestAll Whether to suggest "all" as an option
   * @param exclude Players to exclude from the suggestion
   * @return Stream of suggestions
   */
  protected Stream<String> suggestOnlinePlayers(Player p, String[] args, int currArg, boolean suggestAll, List<Player> exclude) {
    boolean canSeeVanished = PlayerPermission.COMMAND_VANISH_BYPASS.has(p);

    Stream<? extends Player> players = Bukkit.getOnlinePlayers()
      .stream()
      .filter(n -> !exclude.contains(n));

    // Filter out vanished players if the invoker cannot see vanished players
    if (vanish != null && !canSeeVanished)
      players = players.filter(n -> !vanish.isVanished(n));

    Stream<String> names = players
      .map(Player::getDisplayName);

    return (
      suggestAll ? Stream.concat(Stream.of("all"), names) : names
    )
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
   * @return Array of components, where each word and every space is a component
   */
  protected BaseComponent[] buildAdvancedUsage(@Nullable Integer focusedArgument) {
    return buildAdvancedUsage(focusedArgument, false);
  }

  /**
   * Build the usage-string in advanced mode, which supports hover tooltips
   * @param focusedArgument The argument that should be focused using the focus color
   * @param asWords Whether each word and each space needs to be it's own component
   * @return Array of components
   */
  protected BaseComponent[] buildAdvancedUsage(@Nullable Integer focusedArgument, boolean asWords) {
    List<BaseComponent> components = new ArrayList<>();
    String cOth = cfg.get(ConfigKey.ERR_USAGE_COLOR_OTHER).asScalar();

    // Add /command with it's description as a tooltip
    components.add(buildHoverable(cOth + "/" + getName(), cOth + getDescription(), true));

    // Add all it's arguments with their descriptive text as hover-tooltips
    for (int i = 0; i < this.cmdArgs.length; i++) {
      CommandArgument arg = this.cmdArgs[i];

      // Space out args
      components.add(new TextComponent(" "));

      // Decide whether to colorize the argument using normal
      // colors or using the focus color based on it's positional index
      String usage = colorizeUsage(arg.getName(), (focusedArgument != null && focusedArgument == i));

      // Doesn't need to be split up into words
      if (!asWords) {
        components.add(new TextComponent(buildHoverable(usage, cOth + arg.getDescription(), false)));
        continue;
      }

      StringBuilder innerModifier = new StringBuilder();
      int nextPushBegin = 0;

      // Loop through the usage char by char and push space-separated words
      for (int j = 0; j < usage.length(); j++) {
        char c = usage.charAt(j);

        // c is a modify indicator and there hasn't been a push yet
        if (j > 0 && usage.charAt(j - 1) == '§' && nextPushBegin == 0) {
          // There's no modify before this one, reset
          if (j >= 3 && usage.charAt(j - 3) != '§')
            innerModifier.delete(0, innerModifier.length());
          innerModifier.append("§").append(c);
        }

        // Space encountered (or EOL), push text and space separately
        if (c == ' ' || j == usage.length() - 1) {
          int lastChar = c == ' ' ? j - 1 : j;

          components.add(buildHoverable(
            innerModifier + usage.substring(nextPushBegin, lastChar + 1),
            cOth + arg.getDescription(), false
          ));

          if (c == ' ')
            components.add(new TextComponent(" "));

          nextPushBegin = lastChar + 2;
        }
      }
    }

    return components.toArray(BaseComponent[]::new);
  }

  /**
   * Build a hoverable message
   * @param text Message to send
   * @param hover Message to display on hover
   * @param suggest Whether to suggest this text on click
   * @return Built component
   */
  private TextComponent buildHoverable(String text, String hover, boolean suggest) {
    // Build the hoverable text-component and add it to the list
    TextComponent tc = new TextComponent(text);
    tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));

    // Suggest this text on click with all colors stripped off
    if (suggest)
      tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ChatColor.stripColor(text)));

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

  /**
   * Generate an internal error result
   */
  protected void internalError() throws CommandException {
    throw new CommandException(
      cfg.get(ConfigKey.ERR_INTERNAL).withPrefix().asScalar()
    );
  }

  /////////////////////////// Ensure Permission /////////////////////////////

  /**
   * Ensure that the player has this permission, throw otherwise
   * @param p Target player
   * @param perm Permission to test for
   */
  protected void ensurePermission(Player p, PlayerPermission perm) throws CommandException {
    if (!perm.has(p))
      throw new MissingPermissionException(cfg, perm);
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

  ///////////////////////////// Parsing: Durations ///////////////////////////////

  /**
   * Parse a duration into seconds
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   * @return Parsed duration in seconds
   */
  protected int parseDuration(String[] args, int index, Integer argcFallback) throws CommandException {
    if (index >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new UsageMismatchException(cfg, buildAdvancedUsage(index));
    }

    if (timeUtil == null)
      throw new CommandException("The duration parser is not available.");

    int dur = timeUtil.parseDuration(args[index]);

    if (dur < 0)
      throw new InvalidDurationException(cfg, args[index]);

    return dur;
  }

  ///////////////////////////// Parsing: Numbers ///////////////////////////////

  /**
   * Try to parse a floating point value from a string
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @return Parsed float
   */
  protected float parseFloat(String[] args, int index) throws CommandException {
    return parseFloat(args, index, null);
  }

  /**
   * Try to parse a floating point value from a string
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   * @return Parsed float
   */
  protected float parseFloat(String[] args, int index, Float argcFallback) throws CommandException {
    if (index >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new UsageMismatchException(cfg, buildAdvancedUsage(index));
    }

    try {
      return Float.parseFloat(args[index].replace(",", "."));
    } catch (NumberFormatException e) {
      throw new InvalidFloatException(cfg, args[index]);
    }
  }

  /**
   * Try to parse an integer value from a string
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

  /**
   * Try to parse a UUID value from a string
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @return Parsed UUID
   */
  protected UUID parseUUID(String[] args, int index) throws CommandException {
    if (index >= args.length)
      throw new UsageMismatchException(cfg, buildAdvancedUsage(index));

    try {
      return UUID.fromString(args[index]);
    } catch (IllegalArgumentException e) {
      throw new InvalidUUIDException(cfg, args[index]);
    }
  }

  ////////////////////////////// Parsing: Enum ////////////////////////////////

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
    return parseEnum(enumClass, args, index, argcFallback, (repr, con) -> con.name().equalsIgnoreCase(repr));
  }

  /**
   * Parse an enum's value from a plain string (ignores casing) and provide a fallback
   * in case the argument count isn't sufficient to fetch the required argument
   * @param enumClass Class of the target enum
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   * @param equalityChecker Function which checks if a enum-constant is equal to a string representation
   * @return Parsed enum value
   */
  protected<T extends Enum<T>> T parseEnum(Class<T> enumClass, String[] args, int index, T argcFallback, BiFunction<String, T, Boolean> equalityChecker) throws CommandException {
    if (index >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new UsageMismatchException(cfg, buildAdvancedUsage(index));
    }

    // Find the enum constant by it's name
    for (T constant : enumClass.getEnumConstants()) {
      // Invoke the parser
      boolean isMatch = equalityChecker.apply(args[index], constant);
      if (isMatch)
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
   * @param argcFallback Fallback for when the arg-count isn't sufficient
   * @return String containing space separated, joined arguments
   */
  protected String argspan(String[] args, int from, int to, @Nullable String argcFallback) throws CommandException {
    if (from >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new UsageMismatchException(cfg, buildAdvancedUsage(from));
    }

    if (to >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new UsageMismatchException(cfg, buildAdvancedUsage(to));
    }

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
    return argspan(args, from, args.length - 1, null);
  }

  /**
   * Collect a string that spans over multiple arguments till the end
   * @param args Array of arguments
   * @param from Starting index
   * @param argcFallback Fallback for when the arg-count isn't sufficient
   * @return String containing space separated, joined arguments
   */
  protected String argvar(String[] args, int from, String argcFallback) throws CommandException {
    return argspan(args, from, args.length - 1, argcFallback);
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

  /**
   * Get all registered commands
   */
  public static Collection<APlayerCommand> getCommands() {
    return registeredCommands.values();
  }
}
