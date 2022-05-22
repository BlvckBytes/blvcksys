package me.blvckbytes.blvcksys.config;

import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.util.Tuple;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
public class ConfigValue {

  // What marks a variable within the config file?
  private static final String varMakerS = "{{";
  private static final String varMakerE = "}}";

  // Whether or not to ignore the casing of variable names
  private static final boolean ignoreVarCasing = true;

  // Unmodified lines of text read from the config
  private final List<String> lines;

  // Variable names and their values that need to be substituted
  // Names are translated to a pattern when added to the instance
  private final Map<String, Tuple<Pattern, String>> vars;

  // Global prefix value
  private final String prefix;

  // Color palette
  private final String palette;

  // Prefix mode, 'N' = none, 'F' = first line, 'A' = all lines
  private char prefixMode;

  /**
   * Create a new config value builder by a string value
   * @param str String value
   * @param prefix Prefix for optional appending
   * @param palette Color palette characters
   */
  public ConfigValue(String str, String prefix, String palette) {
    this(List.of(str), prefix, palette);
  }

  /**
   * Create a new config value builder by multiple lines
   * @param lines List of lines
   * @param prefix Prefix for optional appending
   * @param palette Color palette characters
   */
  public ConfigValue(List<String> lines, String prefix, String palette) {
    this.lines = lines;
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
    int flags = Pattern.LITERAL | (ignoreVarCasing ? Pattern.CASE_INSENSITIVE : 0);
    this.vars.put(
      name.toLowerCase(),
      new Tuple<>(
        Pattern.compile(varMakerS + name + varMakerE, flags),
        value == null ? "null" : value.toString()
      )
    );
    return this;
  }

  /**
   * Apply an external map of variables all at once
   * @param variables Map of variables
   */
  public ConfigValue withVariables(Map<String, Tuple<Pattern, String>> variables) {
    this.vars.putAll(variables);
    return this;
  }

  /**
   * Export all currently known variables
   * @return Map of variables
   */
  public Map<String, Tuple<Pattern, String>> exportVariables() {
    return Collections.unmodifiableMap(this.vars);
  }

  /**
   * Build as a scalar value using newlines for line separation
   * @return String value
   */
  public String asScalar() {
    return asScalar("\n");
  }

  /**
   * Build as a scalar value using a custom string for line separation
   * @param sep Custom line separator
   * @return String value
   */
  public String asScalar(String sep) {
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);

      // Separate lines
      if (i != 0) {
        // Reset colors between lines
        result.append("§r");
        result.append(sep);
      }

      // Add prefix based on previous selection
      if (prefixMode == 'F' && i == 0 || prefixMode == 'A')
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
      .map(this::transformLine)
      .toList();
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
    for (Map.Entry<String, Tuple<Pattern, String>> var : vars.entrySet()) {
      Tuple<Pattern, String> t = var.getValue();

      // Escape dollars before applying the regex replace, unescape afterwards
      // This avoids illegal group reference exceptions
      input = t.a().matcher(input).replaceAll(t.b().replace("$", "\\$")).replace("\\$", "$");
    }

    return input;
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
    return new ConfigValue("", value, "");
  }
}
