package me.blvckbytes.blvcksys.config;

public interface IConfig {

  /**
   * Get a value by it's config key
   * @param key Key to identify the value
   */
  ConfigValue get(ConfigKey key);

}
