package me.blvckbytes.blvcksys.util;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/24/2022

  Stringify all declared fields of an object to make it easily logable in
  a recursive fashion while limiting the maximum depth.
*/
@AutoConstruct
public class ObjectStringifier {

  private final IConfig cfg;
  private final ILogger logger;

  public ObjectStringifier(
    @AutoInject IConfig cfg,
    @AutoInject ILogger logger
  ) {
    this.cfg = cfg;
    this.logger = logger;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  /**
   * Turn an object into a human readable string, if possible
   * @param o Object to stringify
   * @param depth How deep to stringify list or array elements if they're objects
   *
   * @return String representation or null if it's an object
   */
  public String stringifyObject(Object o, int depth) {
    // Directly stringify null values
    if (o == null)
      return "null";

    Class<?> c = o.getClass();

    String otherColor = cfg.get(ConfigKey.INJECT_EVENT_COLOR_OTHER).asScalar();
    String valueColor = cfg.get(ConfigKey.INJECT_EVENT_COLOR_VALUES).asScalar();

    // Return the string wrapped in quotes
    // Add the value color after the actual value to account for colored strings
    // Also escape backslashes for better readability
    if (o instanceof String)
      return "\"" + stripEscapeSequences(o.toString()) + valueColor + "\"";

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

      // Try to get into the "iterable"
      List<?> list;
      try {
        list = (List<?>) (isList ? o : Arrays.asList((Object[]) o));
      } catch (Exception e) {
        list = List.of("<error>");
      }

      // Iterate list or list from array
      for (int i = 0; i < list.size(); i++) {
        Object curr = list.get(i);
        Object res = stringifyObject(curr, depth);

        // Could not stringify locally
        if (res == null) {
          String tarName = curr.getClass().getSimpleName();

          // Depth remains, try to reach out to the object stringifier
          if (depth > 0)
            res = "%s%s(%s%s)".formatted(
              otherColor, tarName, valueColor,
              stringifyObjectProperties(curr, depth - 1)
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

    // Is an optional
    if (o instanceof Optional<?> opt) {
      // Empty, just write placeholder
      if (opt.isEmpty())
        return "<empty>";

      // Stringify it's contents
      else
        o = opt.get();
    }

    // Complex object, call object stringifier
    String sub = stringifyObjectProperties(o, depth);

    return sub != null ? "%s%s(%s%s)".formatted(
      otherColor, c.getSimpleName(), valueColor, sub
    ) : "<%s>".formatted(c.getSimpleName());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Stringify an object's properties into a comma separated list
   * @param o Object to query
   * @param depth Levels of recursion to allow when stringifying object fields
   * @return Built comma separated list string or null if depth is used up
   */
  private String stringifyObjectProperties(Object o, int depth) {
    // Depth used up
    if (depth <= 0)
      return null;

    // Object is null
    if (o == null)
      return "null";

    StringBuilder props = new StringBuilder();

    String otherColor = cfg.get(ConfigKey.INJECT_EVENT_COLOR_OTHER).asScalar();
    String valueColor = cfg.get(ConfigKey.INJECT_EVENT_COLOR_VALUES).asScalar();

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
          // I am intentionally not logging exceptions here, as it may pollute fast logs
          continue;
        }

        // Call to resolve this object into a simple string (no object field walking)
        Object tar = f.get(o);
        String str = stringifyObject(tar, depth - 1);

        // Not an "easy" stringify
        if (str == null) {
          String tarName = tar.getClass().getSimpleName();

          // Call recursively
          String sub = stringifyObjectProperties(tar, depth - 1);

          str = sub != null ? "%s%s(%s%s)".formatted(
            otherColor, tarName, valueColor, sub
          ) : "<%s>".formatted(tarName);
        }

        // Stringify and append with leading comma, if applicable
        props
          .append(i == 0 ? "" : otherColor + ", ")
          .append(valueColor)
          .append(str);
      }
    } catch (Exception e) {
      logger.logError(e);
    }

    // Re-set the colors at the end
    return props + "§r";
  }

  /**
   * Strip all escape sequences from a string
   * @param input Input string
   * @return String without escape sequences
   */
  private String stripEscapeSequences(String input) {
    StringBuilder sb = new StringBuilder();

    // Filter characters
    for (char c : input.toCharArray()) {
      if (c == '\n')
        sb.append("§r<nl>");

      if (c == '\t')
        sb.append("§r<tab>");

      // Strip escape sequences
      if (c < 32)
        continue;

      // Append this char to the result
      sb.append(c);
    }

    return sb.toString();
  }
}
