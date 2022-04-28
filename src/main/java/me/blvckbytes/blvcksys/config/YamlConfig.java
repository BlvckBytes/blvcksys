package me.blvckbytes.blvcksys.config;

import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Load a yaml configuration from it's file into memory and initialize all
  known config keys from the ConfigKey enum, then save to file again.
  Allows for quick access of Strings and Lists of Strings by using ConfigValue.
*/
@AutoConstruct
public class YamlConfig implements IConfig, IAutoConstructed {

  // Loaded configuration and it's corresponding file handle
  private YamlConfiguration cfg = null;
  private File yf = null;

  // Global prefix string, global palette, load ahead of time as it's used quite often
  private String prefix, palette;

  private final JavaPlugin plugin;

  public YamlConfig(
    @AutoInject JavaPlugin plugin
  ) {
    this.plugin = plugin;

    // Build a fallback for the prefix
    this.prefix = "[" + plugin.getDescription().getName() + "] ";

    // Palette fallback: no colors
    this.palette = "";

    this.load();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  @SuppressWarnings("unchecked")
  public ConfigValue get(ConfigKey key) {
    // Config is not yet loaded
    if (cfg == null)
      return ConfigValue.makeEmpty();

    // Key unknown
    Object val = cfg.get(key.toString());
    if (val == null)
      return ConfigValue.makeEmpty();

    // Is a list
    Class<?> valC = val.getClass();
    if (List.class.isAssignableFrom(valC))
      return new ConfigValue((List<String>) val, prefix, palette);

    // Is a scalar
    else
      return new ConfigValue(val.toString(), prefix, palette);
  }

  @Override
  public void cleanup() {
    // Nothing to do here (yet)
  }

  @Override
  public void initialize() {
    // Nothing to do here (yet)
  }

  //=========================================================================//
  //                               Utilities                                 //
  //=========================================================================//

  /**
   * Load the config from the corresponding file
   */
  private void load() {
    try {
      File df = plugin.getDataFolder();

      // Create data folder if absent
      if (!df.exists())
        if (!df.mkdirs())
          throw new RuntimeException("Could not create data-folder to store the config in");

      yf = new File(df.getAbsolutePath() + "/config.yml");

      // Create file if absent
      if (!yf.exists())
        if (!yf.createNewFile())
          throw new RuntimeException("Could not create config file");

      // Load configuration from file
      cfg = YamlConfiguration.loadConfiguration(yf);

      // Initialize config (appending missing keys)
      // Save on diffs
      if (ensureEntries())
        save();

      // Load the palette
      // No variables or palettes will be available for interpolation
      this.palette = new ConfigValue(
        cfg.getString(ConfigKey.PALETTE.toString()), "", ""
      ).asScalar();

      // Load the prefix ahead of time
      // Already supply the palette
      this.prefix = new ConfigValue(
        cfg.getString(ConfigKey.PREFIX.toString()), "", palette
      ).asScalar();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Initialize the yaml config with all values from the enum
   * @return True if items were added, false otherwise
   */
  private boolean ensureEntries() {
    if (cfg == null)
      return false;

    // Loop all available enum entries and keep track of differences to the config file
    boolean diffs = false;
    for (ConfigKey key : ConfigKey.values()) {
      // Only set the key when it's missing
      if (!cfg.contains(key.toString())) {
        String def = key.getDefaultValue();

        // Set string list
        if (def.contains("\n"))
          cfg.set(key.toString(), Arrays.asList(def.split("\n")));

        // Set scalar string
        else
          cfg.set(key.toString(), def);

        diffs = true;
      }
    }

    return diffs;
  }

  /**
   * Save the local yaml config into it's file
   */
  private void save() {
    // No config loaded yet
    if (cfg == null || yf == null)
      return;

    try {
      // Save config using the file handle
      cfg.save(yf);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
