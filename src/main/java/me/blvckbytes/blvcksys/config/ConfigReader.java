package me.blvckbytes.blvcksys.config;

import lombok.RequiredArgsConstructor;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import net.minecraft.util.Tuple;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

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
            Enchantment e = parseEnchantment(data[0]).orElse(null);
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
    // Get all available colors from the color class's list of constant fields
    try {
      List<Field> constants = Arrays.stream(Color.class.getDeclaredFields())
        .filter(field -> field.getType().equals(Color.class) && Modifier.isStatic(field.getModifiers()))
        .toList();

      for (Field constant : constants) {
        if (constant.getName().equalsIgnoreCase(value))
          return Optional.of((Color) constant.get(null));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

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
   * Parse a bukkit-enchantment based on it's internal constant names
   * @param name Value to parse
   * @return Parsed enchantment, empty if the input is not a known enchantment name
   */
  private Optional<Enchantment> parseEnchantment(String name) {
    // Get all available enchantments from the abstract enchantment class's list of constant fields
    try {
      List<Field> constants = Arrays.stream(Enchantment.class.getDeclaredFields())
        .filter(field -> field.getType().equals(Enchantment.class) && Modifier.isStatic(field.getModifiers()))
        .toList();

      for (Field constant : constants) {
        if (constant.getName().equalsIgnoreCase(name))
          return Optional.of((Enchantment) constant.get(null));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
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
