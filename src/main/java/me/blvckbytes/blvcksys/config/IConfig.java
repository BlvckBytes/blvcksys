package me.blvckbytes.blvcksys.config;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Public interfaces which the Configuration provides to other consumers.
*/
public interface IConfig {

  /**
   * Get a value by it's config key
   * @param key Key to identify the value
   */
  ConfigValue get(ConfigKey key);

}
