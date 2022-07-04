package me.blvckbytes.blvcksys.config;

import com.google.common.primitives.Primitives;
import me.blvckbytes.blvcksys.config.sections.CSAlways;
import me.blvckbytes.blvcksys.config.sections.CSIgnore;
import me.blvckbytes.blvcksys.config.sections.ItemStackSection;
import me.blvckbytes.blvcksys.config.sections.CSMap;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.configuration.MemorySection;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/22/2022

  A wrapper for the basic IConfig which provides reading complex
  values by their key and handles all possible cases when parsing.
*/
public class ConfigReader {

  private final IConfig cfg;
  private final String path;
  private final @Nullable ILogger logger;
  private final Map<String, Object> parseCache;

  public ConfigReader(
    IConfig cfg, String path,
    @Nullable ILogger logger
  ) {
    this.cfg = cfg;
    this.path = path;
    this.logger = logger;
    this.parseCache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  /**
   * Parse a configuration section into a matching internal model by reviving
   * known data-types and walking the whole data-structure recursively, if necessary
   * @param key Key to start parsing from (inclusive), null means top level (full file)
   * @param type Internal model to parse into
   * @return Optional parsed model, if the key existed
   */
  @SuppressWarnings("unchecked")
  public<T extends Object> Optional<T> parseValue(@Nullable String key, Class<T> type, boolean cache) {
    if (cache && parseCache.containsKey(key))
      return Optional.of((T) parseCache.get(key));

    return parseValueSub(key, type, null, false, false)
      .map(v -> {
        if (cache)
          parseCache.put(key, v);
        return v;
      });
  }

  /**
   * Recursive sub-routine with extra parameters
   */
  @SuppressWarnings("unchecked")
  private<T extends Object> Optional<T> parseValueSub(@Nullable String key, Class<T> type, Field f, boolean withinArray, boolean ignoreMissing) {
    boolean isSection = AConfigSection.class.isAssignableFrom(type);

    // Null keys mean root level scope
    if (key == null)
      key = "";

    // If the type is not within an array (as emptyness is used to find list-ends) and
    // the type is not just a scalar value, skip this check, as empty leaf objects should always be initialized
    if (cfg.get(path, key).isEmpty() && (!isSection || withinArray) && !ignoreMissing)
      return Optional.empty();

    // Is a map, parse all keys of this section
    if (Map.class.isAssignableFrom(type) && f != null) {
      CSMap kvInfo = f.getAnnotation(CSMap.class);

      MemorySection ms = get(key)
        .map(cv -> cv.asScalar(MemorySection.class))
        .orElse(null);

      if (kvInfo == null || ms == null)
        return Optional.empty();

      Map<Object, Object> items = new HashMap<>();

      // Iterate all keys of this section
      for (String msKey : ms.getKeys(false)) {
        // Value type unparsable
        Object v = parseValue(join(key, msKey), kvInfo.v(), false).orElse(null);
        if (v == null)
          continue;

        // Key type unparsable
        Object parsedKey = ConfigValue.immediate(msKey).asScalar(kvInfo.k());
        if (parsedKey == null)
          continue;

        items.put(parsedKey, v);
      }

      // Only set if there are actually items available
      if (items.size() > 0)
        return Optional.of(type.cast(items));

      return Optional.empty();
    }

    // Is an array, multiple elements of the same type in a sequence
    if (type.isArray() || List.class.isAssignableFrom(type)) {
      Class<?> arrType = type.getComponentType();

      // Try to fetch as many values of the list as possible, until the end is reached
      List<Object> items = new ArrayList<>();
      for (int i = 0; i < Integer.MAX_VALUE; i++) {
        Optional<?> v = parseValueSub(key + "[" + i + "]", (Class<? extends AConfigSection>) arrType, f, true, false);

        // End of list reached, no more items available
        if (v.isEmpty())
          break;

        items.add(v.get());
      }

      // Only set if there are actually items available
      if (items.size() > 0)
        return Optional.of(
          type.isArray() ?
            // Store as array
            type.cast(items.toArray((Object[]) Array.newInstance(arrType, 1))) :
            // Store as list
            type.cast(items)
        );

      return Optional.empty();
    }

    // Is a configuration section, which means each field will be parsed
    // separately, supporting for recursion
    if (isSection) {
      try {
        AConfigSection res = (AConfigSection) type.getConstructor().newInstance();

        List<Field> fields = Arrays.stream(type.getDeclaredFields())
          .sorted((a, b) -> {
            if (a.getType() == Object.class && b.getType() == Object.class)
              return 0;

            // Objects are "greater", so they'll be last when sorting ASC
            return a.getType() == Object.class ? 1 : -1;
          })
          .toList();

        for (Field field : fields) {
          // Ignore fields marked for ignore
          if (field.getAnnotation(CSIgnore.class) != null)
            continue;

          field.setAccessible(true);

          String fName = field.getName();
          Class<?> fType = field.getType();
          String fKey = join(key, fName);

          // Try to transform the type by letting the class decide at runtime
          if (fType == Object.class)
            fType = res.runtimeDecide(fName);

          // Is another config section and thus needs recursion
          if (AConfigSection.class.isAssignableFrom(fType)) {
            // A field is marked as always by either being directly marked, or by being the member of a marked class
            boolean isAlways = field.isAnnotationPresent(CSAlways.class) || type.isAnnotationPresent(CSAlways.class);

            Object v = parseValueSub(fKey, (Class<? extends AConfigSection>) fType, field, false, isAlways).orElse(null);
            if (v != null)
              field.set(res, v);

            continue;
          }

          // Initially try to parse the value
          Class<?> ffType = fType;
          Object v = parseValueSub(fKey, fType, field, false, false).orElse(null);

          // Failed, try to ask for a default value
          if (v == null)
            v = res.defaultFor(ffType, fName);

          if (v != null)
            field.set(res, v);
        }

        return Optional.of(type.cast(res));
      } catch (Exception e) {
        if (logger == null)
          e.printStackTrace();
        else
          logger.logError(e);
      }
    }

    // Requested an ItemStackBuilder, which always comes without
    // variable processing when parsed this way
    if (type == ItemStackBuilder.class) {
      ItemStackSection is = parseValue(key, ItemStackSection.class, false).orElse(null);
      return is == null ? Optional.empty() : Optional.of(type.cast(is.asItem(null)));
    }

    // Since ConfigValue scalars always work with boxed types, box at this point
    type = Primitives.wrap(type);

    // Try to use ConfigValue's internal casting mechanism
    ConfigValue cv = get(key).orElse(null);
    if (cv != null) {
      // Set the scalar value, only if it's type matches
      Object v = cv.asScalar(type);
      if (v != null && type.isAssignableFrom(v.getClass()))
        return Optional.of(type.cast(v));
    }

    return Optional.empty();
  }

  /**
   * Get a config-value from the file of this reader
   * @param key Key to retrieve
   */
  public Optional<ConfigValue> get(String key) {
    return cfg.get(path, key);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Join two keys with a separating dot and handle all cases
   * @param keyA Key A of the result
   * @param keyB Key B of the result
   * @return A concatenated with B
   */
  private String join(String keyA, String keyB) {
    if (keyA.isBlank())
      return keyB;

    if (keyB.isBlank())
      return keyA;

    if (keyA.endsWith(".") && keyB.startsWith("."))
      return keyA + keyB.substring(1);

    if (keyA.endsWith(".") || keyB.startsWith("."))
      return keyA + keyB;

    return keyA + "." + keyB;
  }
}