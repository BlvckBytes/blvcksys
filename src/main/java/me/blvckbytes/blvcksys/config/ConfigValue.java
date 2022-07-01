package me.blvckbytes.blvcksys.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/25/2022

  Represents a configuration value that can either be retrieved as a
  scalar String or as a List of Strings, where each String represents a
  line of the value. The main prefix can be added to each line, to the
  first line or to no lines at all. Templated values may be interpolated
  with a map of variables. These maps can be imported and exported for
  use accross multiple values. For convenience, the Stream and the
  TextComponent representations are also offered to the consumer. The color
  palette can be accessed in templates by $0...$9
*/
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigValue {

  // Decimal format used when encountering double variables
  private static final DecimalFormat DECIMAL_FORMAT;

  static {
    DECIMAL_FORMAT = (DecimalFormat) NumberFormat.getInstance(Locale.US);
    DECIMAL_FORMAT.applyPattern("0.00");
  }

  // Unmodified lines read from the config
  private final List<Object> lines;

  // Variable names and their values that need to be substituted
  // Names are translated to a pattern when added to the instance
  private final Map<String, String> vars;

  // Global prefix value
  private final String prefix;

  // Color palette
  private final String palette;

  // Prefix mode, 'N' = none, 'F' = first line, 'A' = all lines
  private char prefixMode;

  /**
   * Create a new config value builder by a value
   * @param val Value
   * @param prefix Prefix for optional appending
   * @param palette Color palette characters
   */
  public ConfigValue(Object val, String prefix, String palette) {
    this(List.of(val), prefix, palette);
  }

  /**
   * Create a new config value builder by multiple lines
   * @param lines List of lines
   * @param prefix Prefix for optional appending
   * @param palette Color palette characters
   */
  public ConfigValue(List<Object> lines, String prefix, String palette) {
    this.lines = new ArrayList<>(lines);
    this.prefix = prefix;
    this.palette = palette;
    this.prefixMode = 'N';
    this.vars = new HashMap<>();
  }

  /**
   * Add a prefix to the start of the first line of a resulting string
   */
  public ConfigValue withPrefix() {
    this.prefixMode = 'F';
    return this;
  }

  /**
   * Add a prefix to the start of every line of a resulting string
   */
  public ConfigValue withPrefixes() {
    this.prefixMode = 'A';
    return this;
  }

  /**
   * Add a variable to the template of this value
   * @param name Name of the variable
   * @param value Value of the variable
   */
  public ConfigValue withVariable(String name, @Nullable Object value) {
    this.vars.put(
      name.toLowerCase(),
      value == null ? "null" : stringifyVariable(value)
    );
    return this;
  }

  /**
   * Add a variable to the template of this value
   * @param name Name of the variable
   * @param value Value of the variable
   * @param suffix Suffix to add to the value
   */
  public ConfigValue withVariable(String name, @Nullable Object value, String suffix) {
    this.vars.put(
      name.toLowerCase(),
      value == null ? "null" : (stringifyVariable(value) + suffix)
    );
    return this;
  }

  /**
   * Apply an external map of variables all at once
   * @param variables Map of variables
   */
  public ConfigValue withVariables(@Nullable Map<String, String> variables) {
    if (variables != null)
      this.vars.putAll(variables);
    return this;
  }

  /**
   * Export all currently known variables
   * @return Map of variables
   */
  public Map<String, String> exportVariables() {
    return Collections.unmodifiableMap(this.vars);
  }

  /**
   * Join this value with another value in place, by joining all lines and
   * variables, where the variables of other may override entries of this.
   * The prefix, the palette and the prefix mode are not updated and
   * remain as they currently are.
   * @param other Value to join with
   * @param condition Contition which has to evaluate to true in order to perform the join
   */
  public ConfigValue joinWith(Supplier<ConfigValue> other, boolean condition) {
    if (!condition)
      return this;

    ConfigValue cv = other.get();
    this.vars.putAll(cv.exportVariables());
    this.lines.addAll(cv.lines);
    return this;
  }

  /**
   * Build as a scalar by stringifying values and joining
   * them using newlines for line separation
   * @return String value
   */
  public String asScalar() {
    return asScalar("\n");
  }

  /**
   * Get the first available value from the list as a
   * scalar of a specific type by trying to cast
   * @param type Required type
   * @return Cast type or null
   */
  public<T> @Nullable T asScalar(Class<T> type) {
    try {
      if (lines.size() != 0)
        return cast(lines.get(0), type).orElse(null);
    } catch (ClassCastException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Build as a scalar value using a custom string for line separation
   * @param sep Custom line separator
   * @return String value
   */
  public String asScalar(String sep) {
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i).toString();

      // Separate lines
      if (i != 0) {
        // Reset colors between lines
        result.append("§r");
        result.append(sep);
      }

      // Add prefix based on previous selection on non-empty strings
      if (prefixMode == 'F' && i == 0 || prefixMode == 'A' && !line.isBlank())
        result.append(prefix);

      // Append the actual line after transformation
      result.append(transformLine(line));
    }

    return result.toString();
  }

  /**
   * Build as a component using a custom string for line separation
   * @param sep Custom line separator
   * @return Component value
   */
  public TextComponent asComponent(String sep) {
    return new TextComponent(asScalar(sep));
  }

  /**
   * Build as a component using newlines for line separation
   * @return Component value
   */
  public TextComponent asComponent() {
    return asComponent("\n");
  }

  /**
   * Build as a list of strings, as they were defined in the config.
   * This means that a scalar results in a list of length 1
   * @return List of strings
   */
  public List<String> asList() {
    return lines.stream()
      .map(line -> this.transformLine(line.toString()))
      .map(line -> Arrays.asList(line.split("\n")))
      .reduce(new ArrayList<>(), (a, b) -> {
        a.addAll(b);
        return a;
      });
  }

  /**
   * Get the local list by casting every element to the provided
   * type, where mismatching entries are skipped
   * @param type Required type
   * @return List of casted values
   */
  public<T> List<T> asList(Class<T> type) {
    List<T> buf = new ArrayList<>();

    for (Object o : lines) {
      try {
        cast(o, type).ifPresent(buf::add);
      } catch (ClassCastException ignored) {}
    }

    return buf;
  }

  /**
   * Get the local list as a set by casting every element to the provided
   * type, where mismatching entries are skipped
   * @param type Required type
   * @return Set of casted values
   */
  public<T> Set<T> asSet(Class<T> type) {
    Set<T> buf = new HashSet<>();

    for (Object o : lines) {
      try {
        cast(o, type).ifPresent(buf::add);
      } catch (ClassCastException ignored) {}
    }

    return buf;
  }

  /**
   * Shorthand for {@link #asList()}, just in the format of a stream
   * @return List of strings
   */
  public Stream<String> asStream() {
    return asList().stream();
  }

  /**
   * Transform a line into a state where it can be handed back to the
   * caller (replaces colors and variables)
   * @param input Input string
   * @return Transformed result
   */
  private String transformLine(String input) {
    // Translate the color codes first, since no variables should ever introduce color.
    // Then apply the palette and last but not least the variables, so they don't get transformed.
    return applyVariables(
      applyPalette(
        ChatColor.translateAlternateColorCodes('&', input)
      )
    );
  }

  /**
   * Apply color palette placeholders ($0...$9)
   * @param input Input string
   * @return Transformed result
   */
  private String applyPalette(String input) {
    StringBuilder res = new StringBuilder();

    for (int i = 0; i < input.length(); i++) {
      char curr = input.charAt(i);

      // Not a possible palette notation
      if (i == 0 || input.charAt(i - 1) != '$') {
        res.append(curr);
        continue;
      }

      // Not a number, reset color
      if (curr < 48 || curr > 57)
        curr = 'r';

      int index = curr - 48;

      // Palette character not found, reset color
      if (index >= palette.length())
        curr = 'r';

      else
        // Use palette character as color
        curr = palette.charAt(index);

      // Substitute with the corresponding color notation
      res.append(curr);
      res.setCharAt(i - 1, '§');
    }

    return res.toString();
  }

  /**
   * Substitutes all registered variables into the string's placeholders
   * @param input Input string
   * @return Transformed result
   */
  private String applyVariables(String input) {
    StringBuilder sb = new StringBuilder();

    int startIndInp = -1, startIndSb = -1;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      sb.append(c);

      if (i == input.length() - 1)
        break;

      char n = input.charAt(i + 1);

      // Store the possible variable begin marker
      if (c == '{' && n == '{') {
        startIndInp = i;
        startIndSb = sb.length() - 1;
        continue;
      }

      // Substitute the variable value
      if (c == '}' && n == '}') {
        String name = input.substring(startIndInp + 2, i);
        String value = vars.get(name);

        // Variable found, substitute
        if (value != null) {
          sb.delete(startIndSb, sb.length());

          // Find the last color specified
          String color = ChatColor.getLastColors(sb.toString());

          // Apply affecting colors on all lines of the variable
          sb.append(
            Arrays.stream(value.split("\n"))
              .collect(Collectors.joining("\n" + color, color, ""))
          );

          // Skip the second closing bracket
          i++;
        }

        // End of input reached
        if (i == input.length() - 1)
          break;

        startIndSb = startIndInp = -1;
      }
    }

    return sb.toString();
  }

  /**
   * Create a carbon copy of this config value
   */
  public ConfigValue copy() {
    return new ConfigValue(new ArrayList<>(lines), new HashMap<>(vars), prefix, palette, prefixMode);
  }

  @Override
  public String toString() {
    return asScalar();
  }

  /**
   * Make a new empty instance
   */
  public static ConfigValue makeEmpty() {
    return new ConfigValue("", "", "");
  }

  /**
   * Make a new instance from an immediate value
   * @param value Immediate value
   */
  public static ConfigValue immediate(String value) {
    return new ConfigValue(value, "", "");
  }

  /**
   * Tries to "cast" an object read from the config using get() into the
   * required type and responds with an empty result if the conversion is impossible
   * @param value Value to convert
   * @param type Type to convert to
   */
  private<T> Optional<T> cast(Object value, Class<T> type) {
    try {

      String stringValue = applyVariables(value.toString().trim());

      // Automatic enum parsing
      if (type.isEnum()) {
        for (T ec : type.getEnumConstants()) {
          if (((Enum<?>) ec).name().equalsIgnoreCase(stringValue))
            return Optional.of(ec);
        }

        return Optional.empty();
      }

      // Automatic enchantment parsing
      if (type == Enchantment.class) {
        Field target = Arrays.stream(Enchantment.class.getDeclaredFields())
          .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == Enchantment.class)
          .filter(f -> f.getName().equalsIgnoreCase(stringValue))
          .findFirst()
          .orElse(null);

        if (target == null)
          return Optional.empty();

        return Optional.of(type.cast(target.get(null)));
      }

      // Automatic material parsing
      if (type == Material.class)
        return Optional.of(type.cast(Material.valueOf(stringValue)));

      // Automatic itemflag parsing
      if (type == ItemFlag.class)
        return Optional.of(type.cast(ItemFlag.valueOf(stringValue)));

      // Automatic color parsing
      if (type == Color.class) {
        Field target = Arrays.stream(Color.class.getDeclaredFields())
          .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == Color.class)
          .filter(f -> f.getName().equalsIgnoreCase(stringValue))
          .findFirst()
          .orElse(null);

        // A color with this name existed
        if (target != null)
          return Optional.of(type.cast(target.get(null)));

        // Assume it's an RGB color
        String[] parts = stringValue.split(" ");

        // Malformed
        if (parts.length != 3)
          return Optional.empty();

        // Parse RGB parts
        return Optional.of(type.cast(Color.fromRGB(
          Integer.parseInt(parts[0]),
          Integer.parseInt(parts[1]),
          Integer.parseInt(parts[2])
        )));
      }

      return Optional.of(type.cast(value));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Turn a variable's value into it's string representation
   * @param value Value to stringify
   */
  private String stringifyVariable(Object value) {
    // Doubles should always have two decimal digits
    if (value instanceof Double d)
      return d == 0 ? "0" : DECIMAL_FORMAT.format(d);

    return value.toString();
  }
}
