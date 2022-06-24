package me.blvckbytes.blvcksys.config;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Public interfaces which the Configuration provides to other consumers.
*/
public interface IConfig {

  /**
   * Get a value by it's config key from the main config
   * @param key Key to identify the value
   */
  ConfigValue get(ConfigKey key);

  /**
   * Get a value by it's config key from any available config file
   * @param path Path of the target config file (no leading slash, no .yml)
   * @param key Key to identify the value
   */
  Optional<ConfigValue> get(String path, String key);

  /**
   * Get an advanced config reader for a given file
   * @param path Path of the target config file (no leading slash, no .yml)
   */
  Optional<ConfigReader> reader(String path);
}
