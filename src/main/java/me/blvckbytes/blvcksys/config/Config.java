package me.blvckbytes.blvcksys.config;

import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@AutoConstruct
public class Config implements IConfig, IAutoConstructed {

  // Loaded configuration and it's corresponding file handle
  private YamlConfiguration cfg = null;
  private File yf = null;

  private final JavaPlugin plugin;

  public Config(
    @AutoInject JavaPlugin plugin
  ) {
    this.plugin = plugin;
    this.load();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public String get(ConfigKey key, Object... formatArgs) {
    // Config is not yet loaded
    if (cfg == null)
      return "";

    // Get the key's value
    String val = cfg.getString(key.toString()).formatted(formatArgs);

    // Key not found
    if (val == null)
      return "";

    // Translate color codes before returning
    return ChatColor.translateAlternateColorCodes('&', val);
  }

  @Override
  public String getP(ConfigKey key, Object... formatArgs) {
    return get(ConfigKey.PREFIX) + get(key, formatArgs);
  }

  @Override
  public void cleanup() {
    // Save the file on disabling
    this.save();
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
        cfg.set(key.toString(), key.getDefaultValue());
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
