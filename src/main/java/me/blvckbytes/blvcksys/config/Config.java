package me.blvckbytes.blvcksys.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Config {

  private static final String name = "config";
  private static YamlConfiguration cfg = null;
  private static File yf = null;

  /**
   * Get a string by it's config key and apply format arguments to it, if applicable
   * @param key Key inside the config
   * @param formatArgs Arguments to apply to formatting
   * @return Formatted and color-translated value or null on missing keys
   */
  public static String get(ConfigKey key, Object... formatArgs) {
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

  /**
   * Get a string by it's config key and apply format arguments to it, if applicable, and prepend the prefix
   * @param key Key inside the config
   * @param formatArgs Arguments to apply to formatting
   * @return Formatted and color-translated value or null on missing keys
   */
  public static String getP(ConfigKey key, Object... formatArgs) {
    return get(ConfigKey.PREFIX) + get(key, formatArgs);
  }

  public static void load(JavaPlugin plugin) throws Exception {
    File df = plugin.getDataFolder();

    // Create data folder if absent
    if (!df.exists())
      if (!df.mkdirs())
        throw new RuntimeException("Could not create data-folder to store the config in");

    yf = new File("%s/%s.yml".formatted(df.getAbsolutePath(), name));

    // Create file if absent
    if (!yf.exists())
      if (!yf.createNewFile())
        throw new RuntimeException("Could not create config file");

    // Load configuration from file
    cfg = YamlConfiguration.loadConfiguration(yf);

    // Initialize config (appending missing keys)
    // Save on diffs
    if (initialize())
      save();
  }

  private static boolean initialize() {
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

  private static void save() throws Exception {
    if (cfg == null || yf == null)
      return;
    cfg.save(yf);
  }
}
