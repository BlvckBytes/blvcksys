package me.blvckbytes.blvcksys.util.cmd;

import me.blvckbytes.blvcksys.Main;
import me.blvckbytes.blvcksys.config.Config;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.MCReflect;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
  private String[][] argDescs;
  private static Map<String, APlayerCommand> registeredCommands;

  static {
    registeredCommands = new HashMap<>();
  }

  /**
   * @param name Name of the command
   * @param description Description of the command
   * @param argDescs Mapping
   * @param aliases Aliases the command can also be called by
   */
  public APlayerCommand(String name, String description, String[][] argDescs, String ...aliases) {
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

    // Register this command within the server's command map
    MCReflect.registerCommand(Main.getInst().getDescription().getName(), this);
    registeredCommands.put(name, this);
    Main.logger().logDebug("Registered command /%s using handler %s", name, this.getClass().getName());
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

      case USAGE_MISMATCH -> cs.spigot().sendMessage(buildAdvancedUsage(Config.getP(ConfigKey.ERR_USAGE)));

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
    String cOth = Config.get(ConfigKey.ERR_USAGE_COLOR_OTHER);

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
  public static String colorizeUsage(String vanilla) {
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
   * @param otherColor Color prepended to non-values
   * @param valueColor Color prepended to values
   * @param depth How deep to stringify list or array elements if they're objects
   *
   * @return String representation or null if it's an object
   */
  protected String stringifyObject(Object o, String otherColor, String valueColor, int depth) {
    // Directly stringify null values
    if (o == null)
      return "null";

    Class<?> c = o.getClass();

    // Return the string wrapped in quotes
    if (o instanceof String)
      return "\"" + o + "\"";

    // Just stringify primitives (and their wrappers) or enums
    if (
      c.isPrimitive()
        || c.isEnum()
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
      StringBuilder sb = new StringBuilder(otherColor + "[");

      // Iterate list or list from array
      List<?> list = (List<?>) (isList ? o : Arrays.asList((Object[]) o));
      for (int i = 0; i < list.size(); i++) {
        Object curr = list.get(i);
        Object res = stringifyObject(curr, otherColor, valueColor, depth);

        // Could not stringify locally
        if (res == null) {
          String tarName = curr.getClass().getSimpleName();

          // Depth remains, try to reach out to the object stringifier
          if (depth > 0)
            res = "%s%s(%s%s)".formatted(
              otherColor, tarName, valueColor,
              stringifyObjectProperties(curr, depth - 1, otherColor, valueColor)
            );

          // No more depth, use placeholding indicator
          else
            res = "<%s>".formatted(tarName);
        }

        // Call recursively until a scalar value occurs
        sb
          .append(i == 0 ? "" : otherColor + ", ")
          .append(valueColor)
          .append(
            res
          );
      }

      // Reset color at the end
      sb.append(otherColor).append("]").append("§r");
      return sb.toString();
    }

    // Not "easily" formattable
    return null;
  }

  /**
   * Stringify an object's properties into a comma separated list
   * @param o Object to query
   * @param depth Levels of recursion to allow when stringifying object fields
   * @param otherColor Color prepended to non-values
   * @param valueColor Color prepended to values
   * @return Built comma separated list string
   */
  protected String stringifyObjectProperties(Object o, int depth, String otherColor, String valueColor) {
    StringBuilder props = new StringBuilder();

    try {
      Class<?> cl = o.getClass();
      Field[] fields = cl.getDeclaredFields();

      // This class doesn't contain any fields, search for superclasses
      while (
        // No fields yet
        fields.length == 0 &&

          // Superclass available
          cl.getSuperclass() != null
      ) {
        // Navigate into superclass and list it's fields
        cl = cl.getSuperclass();
        fields = cl.getDeclaredFields();
      }

      // Skip static fields
      fields = Arrays.stream(fields)
        .filter(f -> !Modifier.isStatic(f.getModifiers()))
        .toArray(Field[]::new);

      // Loop all fields of this packet and add them to a comma separated list
      for (int i = 0; i < fields.length; i++) {
        Field f = fields[i];

          // Also access private fields, of course
        try {
          f.setAccessible(true);
        } catch (Exception e) {
          // Could not access this field, skip it
          continue;
        }

        // Call to resolve this object into a simple string (no object field walking)
        Object tar = f.get(o);
        String str = stringifyObject(tar, otherColor, valueColor, depth - 1);

        // Not an "easy" stringify
        if (str == null) {
          String tarName = tar.getClass().getSimpleName();

          // Depth used up
          if (depth == 0)
            // Could not stringify, use placeholder with classname as indicator
            str = "<%s>".formatted(tarName);

          // Call recursively
          else
            str = "%s%s(%s%s)".formatted(
              otherColor, tarName, valueColor,
              stringifyObjectProperties(tar, depth - 1, otherColor, valueColor)
            );
        }

        // Stringify and append with leading comma, if applicable
        props
          .append(i == 0 ? "" : otherColor + ", ")
          .append(valueColor)
          .append(str);
      }
    } catch (Exception e) {
      Main.logger().logError(e);
    }

    // Re-set the colors at the end
    return props + "§r";
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
