package me.blvckbytes.blvcksys.config;

import java.util.List;
import java.util.Optional;

public interface IConfig {

  /**
   * Get a string by it's config key and apply format arguments to it, if applicable
   * @param key Key inside the config
   * @param formatArgs Arguments to apply to formatting
   * @return Formatted and color-translated value or null on missing keys
   */
  String get(ConfigKey key, Object... formatArgs);

  /**
   * Get a list of strings by it's config key
   * @param key Key inside the config
   * @return Color-translated list, empty on missing key
   */
  Optional<List<String>> getL(ConfigKey key);

  /**
   * Get a string by it's config key and apply format arguments to it, if applicable, and prepend the prefix
   * @param key Key inside the config
   * @param formatArgs Arguments to apply to formatting
   * @return Formatted and color-translated value or null on missing keys
   */
  String getP(ConfigKey key, Object... formatArgs);
}
