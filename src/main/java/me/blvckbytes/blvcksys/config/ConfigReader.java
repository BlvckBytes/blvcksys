package me.blvckbytes.blvcksys.config;

import com.google.common.primitives.Primitives;
import lombok.RequiredArgsConstructor;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/22/2022

  A wrapper for the basic IConfig which provides reading complex
  values by their key and handles all possible cases when parsing.
*/
@RequiredArgsConstructor
public class ConfigReader {

  private final IConfig cfg;
  private final String path;
  private final @Nullable IPlayerTextureHandler textureHandler;
  private final @Nullable ILogger logger;

  @SuppressWarnings("unchecked")
  public<T extends AConfigSection> Optional<T> parseValue(String key, Class<T> type) {
    // Key does not exist
    if (cfg.get(path, key).isEmpty())
      return Optional.empty();

    try {
      T res = type.getConstructor().newInstance();

      for (Field f : type.getDeclaredFields()) {
        f.setAccessible(true);

        String fName = f.getName();
        Class<?> fType = Primitives.wrap(f.getType());

        // Try to transform the type by letting the class decide at runtime
        if (fType == Object.class)
          fType = res.runtimeDecide(fName);

        // Parse an item stack using the local utility
        if (fType == ItemStack.class) {
          Object v = getItem(join(key, fName), null).orElse(null);
          if (v != null)
            f.set(res, v);

          continue;
        }

        boolean isConfigValue = AConfigSection.class.isAssignableFrom(fType);
        boolean isEnum = (fType.isEnum() || (hasStaticSelfConstants(fType) && !isConfigValue));

        // Is a value which can be retrieved immediately, doesn't need recursion
        if (
          Primitives.isWrapperType(fType) ||
          fType == String.class ||
          fType == ConfigValue.class ||
          isEnum
        ) {
          ConfigValue cv = get(join(key, fName)).orElse(null);

          if (cv != null) {
            // Requested the vanilla config value as is
            if (fType == ConfigValue.class)
              f.set(res, cv);

            // Try to parse an enum from the value's scalar string repr
            else if (isEnum) {
              Object v = parseEnum(fType, cv.asScalar()).orElse(null);
              if (v != null)
                f.set(res, v);
            }

            // Set the scalar value, only if it's type matches
            else {
              Object v = cv.asScalar(fType, null);
              if (fType.isAssignableFrom(v.getClass()))
                f.set(res, v);
            }
          }

          continue;
        }

        // Is an array, multiple elements of the same type in a sequence
        if (fType.isArray()) {
          Class<?> arrType = f.getType().getComponentType();

          // Unsupported array type
          if (!AConfigSection.class.isAssignableFrom(arrType))
            continue;

          // Try to fetch as many values of the list as possible, until the end is reached
          List<Object> items = new ArrayList<>();
          for (int i = 0; i < Integer.MAX_VALUE; i++) {
            Optional<?> v = parseValue(join(key, fName) + "[" + i + "]", (Class<? extends AConfigSection>) arrType);

            // End of list reached, no more items available
            if (v.isEmpty())
              break;

            items.add(v.get());
          }

          f.set(res, items.toArray((Object[]) Array.newInstance(arrType, 1)));
          continue;
        }

        // Is another config value and thus needs recursion
        if (isConfigValue) {
          Object v = parseValue(join(key, fName), (Class<? extends AConfigSection>) fType).orElse(null);

          if (v != null)
            f.set(res, v);

          continue;
        }

        // Invalid type encountered, leave at default
      }

      return Optional.of(res);
    } catch (Exception e) {
      if (logger == null)
        e.printStackTrace();
      else
        logger.logError(e);
    }

    return Optional.empty();
  }

  /**
   * Checks whether a class declares static constants of it's own type (enum-like)
   * @param c Class to check
   */
  private boolean hasStaticSelfConstants(Class<?> c) {
    return Arrays.stream(c.getDeclaredFields()).anyMatch(field -> field.getType().equals(c) && Modifier.isStatic(field.getModifiers()));
  }

  /**
   * Read an item-stack by it's key
   * @param key Target config key
   * @param variables Optional map of variables applied to internal config values
   * @return ItemStack on success, empty if the key was invalid
   */
  public Optional<ItemStack> getItem(String key, @Nullable Map<String, String> variables) {
    // Not an object
    if (!cfg.nonScalarExists(path, key))
      return Optional.empty();

    ConfigValue name = get(join(key, "name")).map(cv -> cv.withVariables(variables)).orElse(null);
    ConfigValue lore = get(join(key, "lore")).map(cv -> cv.withVariables(variables)).orElse(null);
    ConfigValue flags = get(join(key, "flags")).map(cv -> cv.withVariables(variables)).orElse(null);
    ConfigValue color = get(join(key, "color")).map(cv -> cv.withVariables(variables)).orElse(null);
    ConfigValue enchantments = get(join(key, "enchantments")).map(cv -> cv.withVariables(variables)).orElse(null);
    ConfigValue textures = get(join(key, "textures")).map(cv -> cv.withVariables(variables)).orElse(null);

    int amount = getScalar(join(key, "amount"), Integer.class, 1);
    Material mat = getScalar(join(key, "type"), Material.class, Material.BARRIER);

    return Optional.of(
      new ItemStackBuilder(mat, amount)
        .withName(name, name != null)
        .withLore(lore, lore != null)
        .withFlags(() -> flags.asList(ItemFlag.class), flags != null)
        .withEnchantments(() -> (
          // Try to parse each line using the format <enchantment>:<level>
          enchantments.asList().stream().map(line -> {
            String[] data = line.split(":");

            // Invalid format specified
            if (data.length != 2)
              return null;

            // Try to parse the enchantment by it's constant name and the
            // integer level as a positive number
            Enchantment e = parseEnum(Enchantment.class, data[0]).orElse(null);
            Integer level = parseInt(data[1]).orElse(null);

            // Invalid values
            if (e == null || level == null || level <= 0)
              return null;

            return new Tuple<>(e, level);
          })
          .filter(Objects::nonNull).toList()
        ), enchantments != null)
        .withColor(() -> parseColor(color.asScalar()).orElse(Color.BLACK), color != null)
        .withProfile(() -> textureHandler.getProfileOrDefault(textures.asScalar()), textures != null && textureHandler != null)
        .build()
    );
  }

  /**
   * Get a config-value from the file of this reader
   * @param key Key to retrieve
   */
  public Optional<ConfigValue> get(String key) {
    return cfg.get(path, key);
  }

  /**
   * Get a scalar value or make use of a fallback
   * @param key Target key
   * @param type Type to cast to
   * @param fallback Fallback for error cases
   */
  private<T> T getScalar(String key, Class<T> type, T fallback) {
    ConfigValue cv = get(key).orElse(null);
    if (cv == null)
      return fallback;
    return cv.asScalar(type, fallback);
  }

  /**
   * Parse a bukkit-color by either one if it's predefined constant names, or by the R G B notation
   * @param value Value to parse
   * @return Parsed color, empty on malformed inputs
   */
  private Optional<Color> parseColor(String value) {
    // Try to parse the color by it's name
    Optional<Color> byName = parseEnum(Color.class, value);
    if (byName.isPresent())
      return byName;

    // Assume it's RGB, formatted like: r g b
    String[] data = value.split(" ");

    // Malformed
    if (data.length != 3)
      return Optional.empty();

    Integer r = parseInt(data[0]).orElse(null), g = parseInt(data[1]).orElse(null), b = parseInt(data[2]).orElse(null);

    // Invalid values
    if (r == null || g == null || b == null)
      return Optional.empty();

    return Optional.of(Color.fromRGB(r, g, b));
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
}
