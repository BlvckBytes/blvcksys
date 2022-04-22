package me.blvckbytes.blvcksys.util.cmd;

import me.blvckbytes.blvcksys.Main;
import me.blvckbytes.blvcksys.config.Config;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.MCReflect;
import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

  /**
   * @param name Name of the command
   * @param description Description of the command
   * @param usage Command's usage
   * @param aliases Aliases the command can also be called by
   */
  public APlayerCommand(String name, String description, String usage, String ...aliases) {
    super(name, description, usage, Arrays.asList(aliases));

    // Register this command within the server's command map
    MCReflect.registerCommand(Main.getInst().getDescription().getName(), this);
    Main.logger().logDebug("Registered command /%s using handler %s", name, this.getClass().getName());

    // Also register events if the listener interface has been implemented
    if (this instanceof Listener l)
      Main.getInst().getServer().getPluginManager().registerEvents(l, Main.getInst());
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
      cs.sendMessage(Config.getP(ConfigKey.ERR_NOT_A_PLAYER));
      return false;
    }

    // Relay handling, send the usage if exec was not successful
    CommandResult res = onInvocation((Player) cs, label, args);

    // No error occurred
    if (res.error() == CommandError.NONE)
      return true;

    // Decide on error-response
    switch (res.error()) {
      case PLAYER_NOT_ONLINE -> cs.sendMessage(Config.getP(ConfigKey.ERR_NOT_ONLINE, res.args()));

      case USAGE_MISMATCH -> cs.sendMessage(Config.getP(ConfigKey.ERR_USAGE, colorizedUsage()));

      // Custom error with custom format, [0] is the message and [1]..[n] are the args to format the message
      case CUSTOM_ERROR -> cs.sendMessage(res.args()[0].toString().formatted(Arrays.copyOfRange(res.args(), 1, res.args().length)));

      // Unparsable integer
      case INT_UNPARSEABLE -> cs.sendMessage(Config.getP(ConfigKey.ERR_INTPARSE, res.args()));

      default -> cs.sendMessage(Config.getP(ConfigKey.ERR_INTERNAL));
    }

    return false;
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Colorize the usage string based on the colors specified inside the config
   * @return Colorized usage string
   */
  private String colorizedUsage() {
    // Vanilla usage string
    String vanilla = getUsage();

    // Usage formatting colors
    String cMan = Config.get(ConfigKey.ERR_USAGE_COLOR_MANDATORY);
    String cOpt = Config.get(ConfigKey.ERR_USAGE_COLOR_OPTIONAL);
    String cBra = Config.get(ConfigKey.ERR_USAGE_COLOR_BRACKETS);
    String cOth = Config.get(ConfigKey.ERR_USAGE_COLOR_OTHER);

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
    return new CommandResult(CommandError.NONE);
  }

  /**
   * Generate a usage-mismatch result
   */
  protected CommandResult usageMismatch() {
    return new CommandResult(CommandError.USAGE_MISMATCH);
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
  protected CommandResult customError(String message, Object ...args) {
    // Create a copy of args with message as the first element
    Object[] totalArgs = new Object[args.length + 1];
    totalArgs[0] = message;
    System.arraycopy(args, 0, totalArgs, 1, args.length);
    return new CommandResult(CommandError.CUSTOM_ERROR, totalArgs);
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
   * Turn an object into a human readable string, if possible
   * @param o Object to stringify
   * @return String representation
   */
  protected String stringifyObject(Object o) {
    // Directly stringify null values
    if (o == null)
      return "null";

    Class<?> c = o.getClass();

    // Just stringify primitives (and their wrappers), enums or strings
    if (
      c.isPrimitive()
        || c.isEnum()
        || o instanceof String
        || o instanceof Integer
        || o instanceof Long
        || o instanceof Double
        || o instanceof Float
        || o instanceof Boolean
        || o instanceof Byte
        || o instanceof Short
        || o instanceof Character
    )
      return o.toString();

    // Is an array or a list, format as [...]
    boolean isList = List.class.isAssignableFrom(c);
    if (c.isArray() || isList) {
      StringBuilder sb = new StringBuilder("[");

      // Iterate list or list from array
      List<?> list = (List<?>) (isList ? o : Arrays.asList((Object[]) o));
      for (int i = 0; i < list.size(); i++)
        sb.append(i == 0 ? "" : ", ").append(stringifyObject(list.get(i)));

      sb.append("]");
      return sb.toString();
    }

    // Not "easily" formattable, at least inform about the classname
    return "<" + c.getSimpleName() + ">";
  }
}
