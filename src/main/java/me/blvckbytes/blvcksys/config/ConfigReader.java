package me.blvckbytes.blvcksys.config;

import com.google.common.primitives.Primitives;
import com.mojang.authlib.GameProfile;
import me.blvckbytes.blvcksys.config.sections.ConfigSectionIgnore;
import me.blvckbytes.blvcksys.config.sections.ItemStackSection;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/22/2022

  A wrapper for the basic IConfig which provides reading complex
  values by their key and handles all possible cases when parsing.
*/
public class ConfigReader {

  // Parser registry, mapping the parser output type to the parser function
  private final Map<Class<?>, Function<String, Optional<?>>> typeParsers;

  private final IConfig cfg;
  private final String path;
  private final @Nullable IPlayerTextureHandler textureHandler;
  private final @Nullable ILogger logger;

  public ConfigReader(
    IConfig cfg, String path,
    @Nullable IPlayerTextureHandler textureHandler,
    @Nullable ILogger logger
  ) {
    this.cfg = cfg;
    this.path = path;
    this.textureHandler = textureHandler;
    this.logger = logger;
    this.typeParsers = new HashMap<>();

    registerParsers();
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
  public<T extends Object> Optional<T> parseValue(@Nullable String key, Class<T> type) {
    return parseValueSub(key, type, false);
  }

  /**
   * Recursive sub-routine with extra parameters
   */
  @SuppressWarnings("unchecked")
  private<T extends Object> Optional<T> parseValueSub(@Nullable String key, Class<T> type, boolean withinArray) {
    boolean isSection = AConfigSection.class.isAssignableFrom(type);

    // Null keys mean root level scope
    if (key == null)
      key = "";

    // If the type is not within an array (as emptyness is used to find list-ends) and
    // the type is not just a scalar value, skip this check, as empty leaf objects should always be initialized
    if (cfg.get(path, key).isEmpty() && (!isSection || withinArray))
      return Optional.empty();

    // Since ConfigValue scalars always work with boxed types, box at this point
    type = Primitives.wrap(type);

    // Is a wrapper type, which is a scalar value
    if (Primitives.isWrapperType(type)) {
      ConfigValue cv = get(key).orElse(null);

      if (cv != null) {
        // Set the scalar value, only if it's type matches
        Object v = cv.asScalar(type, null);
        if (type.isAssignableFrom(v.getClass()))
          return Optional.of(type.cast(v));
      }

      return Optional.empty();
    }

    // Is an array, multiple elements of the same type in a sequence
    if (type.isArray()) {
      Class<?> arrType = type.getComponentType();

      // Try to fetch as many values of the list as possible, until the end is reached
      List<Object> items = new ArrayList<>();
      for (int i = 0; i < Integer.MAX_VALUE; i++) {
        Optional<?> v = parseValueSub(key + "[" + i + "]", (Class<? extends AConfigSection>) arrType, true);

        // End of list reached, no more items available
        if (v.isEmpty())
          break;

        items.add(v.get());
      }

      // Only set if there are actually items available
      if (items.size() > 0)
        return Optional.of(type.cast(items.toArray((Object[]) Array.newInstance(arrType, 1))));

      return Optional.empty();
    }

    // Try to find a known parser for the field's type
    Function<String, Optional<?>> parser = typeParsers.get(type);
    if (parser != null) {
      Object v = parser.apply(key).orElse(null);
      if (v != null)
        return Optional.of(type.cast(v));

      return Optional.empty();
    }

    // Is either a true java enum type or has a static self-type constant list
    if (type.isEnum() || hasStaticSelfConstants(type)) {
      ConfigValue cv = get(key).orElse(null);

      if (cv != null) {
        // Try to parse an enum from the value's scalar string repr
        T v = parseEnum(type, cv.asScalar()).orElse(null);
        if (v != null)
          return Optional.of(v);
      }

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

        for (Field f : fields) {
          // Ignore fields marked for ignore
          if (f.getAnnotation(ConfigSectionIgnore.class) != null)
            continue;

          f.setAccessible(true);

          String fName = f.getName();
          Class<?> fType = f.getType();
          String fKey = join(key, fName);

          // Try to transform the type by letting the class decide at runtime
          if (fType == Object.class)
            fType = res.runtimeDecide(fName);

          // Is another config section and thus needs recursion
          if (AConfigSection.class.isAssignableFrom(fType)) {
            Object v = parseValue(fKey, (Class<? extends AConfigSection>) fType).orElse(null);
            if (v != null)
              f.set(res, v);
            continue;
          }

          Object v = parseValue(fKey, fType).orElse(null);
          if (v != null)
            f.set(res, v);
        }

        return Optional.of(type.cast(res));
      } catch (Exception e) {
        if (logger == null)
          e.printStackTrace();
        else
          logger.logError(e);
      }
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
   * Checks whether a class declares static constants of it's own type (enum-like)
   * @param c Class to check
   */
  private boolean hasStaticSelfConstants(Class<?> c) {
    return Arrays.stream(c.getDeclaredFields()).anyMatch(field -> field.getType().equals(c) && Modifier.isStatic(field.getModifiers()));
  }

  /**
   * Parses an "enum" from either true enum constants or static self-typed
   * constant declarations within the specified class
   * @param c Target class
   * @param value String value to parse
   * @return Optional constant, empty if there was no such constant
   */
  @SuppressWarnings("unchecked")
  private<T> Optional<T> parseEnum(Class<T> c, String value) {
    value = value.trim();

    // Parse enums
    if (c.isEnum()) {
      for (T ec : c.getEnumConstants()) {
        if (((Enum<?>) ec).name().equalsIgnoreCase(value))
          return Optional.of(ec);
      }
    }

    // Parse classes with static constants
    else {
      try {
        List<Field> constants = Arrays.stream(c.getDeclaredFields())
          .filter(field -> field.getType().equals(c) && Modifier.isStatic(field.getModifiers()))
          .toList();

        for (Field constant : constants) {
          if (constant.getName().equalsIgnoreCase(value))
            return Optional.of((T) constant.get(null));
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    return Optional.empty();
  }

  /**
   * Try to parse an integer from a string
   * @param input String to parse
   * @return Integer on success, empty on malformed inputs
   */
  private Optional<Integer> parseInt(String input) {
    try {
      return Optional.of(Integer.parseInt(input));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

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

  //=========================================================================//
  //                             Parser Registry                             //
  //=========================================================================//

  /**
   * Register all available parser functions by their output type
   */
  private void registerParsers() {
    typeParsers.put(Color.class, this::parseColor);
    typeParsers.put(ItemStackBuilder.class, this::getItem);
    typeParsers.put(ItemStack.class, key -> this.getItem(key).map(ItemStackBuilder::build));
    typeParsers.put(String.class, key -> get(key).map(ConfigValue::asScalar));
    typeParsers.put(ConfigValue.class, this::get);
    typeParsers.put(GameProfile.class, this::getProfile);
  }

  /**
   * Parse a bukkit-color by either one if it's predefined constant names, or by the R G B notation
   * @param key Key within the config
   * @return Parsed color, empty on malformed inputs
   */
  private Optional<Color> parseColor(String key) {
    // Try to get the config value from this key
    ConfigValue cv = get(key).orElse(null);
    if (cv == null)
      return Optional.empty();

    String value = cv.asScalar();

    // Try to parse the color by it's name
    Optional<Color> byName = parseEnum(Color.class, value);
    if (byName.isPresent())
      return byName;

    // Assume it's RGB, formatted like: r g b
    String[] data = value.split(" ");

    // Malformed
    if (data.length != 3)
      return Optional.empty();

    Integer r = parseInt(data[0]).orElse(null);
    Integer g = parseInt(data[1]).orElse(null);
    Integer b = parseInt(data[2]).orElse(null);

    // Invalid values
    if (r == null || g == null || b == null)
      return Optional.empty();

    return Optional.of(Color.fromRGB(r, g, b));
  }

  /**
   * Parse an item-stack by it's key by parsing all property values and
   * supplying fallbacks if required values are not provided
   * @param key Target config key
   * @return ItemStack on success, empty if the key was invalid
   */
  private Optional<ItemStackBuilder> getItem(String key) {
    ItemStackSection is = parseValue(key, ItemStackSection.class).orElse(null);
    return is == null ? Optional.empty() : Optional.of(is.asItem());
  }

  /**
   * Get the game profile of a player by their name, returns a default steve
   * game profile if the name couldn't be resolved
   * @param key Target config key
   * @return GameProfile on success, empty if the key was invalid
   */
  private Optional<GameProfile> getProfile(String key) {
    // Cannot resolve textures due to missing dependency
    if (textureHandler == null)
      return Optional.empty();

    return get(key)
      .map(ConfigValue::asScalar)
      .map(textureHandler::getProfileOrDefault);
  }
}