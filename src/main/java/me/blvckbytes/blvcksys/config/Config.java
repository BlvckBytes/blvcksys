package me.blvckbytes.blvcksys.config;

import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
  @SuppressWarnings("unchecked")
  public String get(ConfigKey key, Object... formatArgs) {
    // Config is not yet loaded
    if (cfg == null)
      return "";

    // Key unknown
    Object val = cfg.get(key.toString());
    if (val == null)
      return "";

    String value;

    // Is a list
    Class<?> valC = val.getClass();
    if (List.class.isAssignableFrom(valC))
      value = String.join("\n", (List<String>) val);

    // Is a scalar
    else
      value = val.toString();

    // Translate color codes before returning
    return ChatColor.translateAlternateColorCodes('&', value.formatted(formatArgs));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Optional<List<String>> getL(ConfigKey key) {
    // Key unknown
    Object val = cfg.get(key.toString());
    if (val == null)
      return Optional.empty();

    // Is a list
    Class<?> valC = val.getClass();
    if (List.class.isAssignableFrom(valC))
      return Optional.of(
        ((List<String>) val).stream()
          // Translate color codes
          .map(l -> ChatColor.translateAlternateColorCodes('&', l))
          .toList()
      );

    // Not a list
    return Optional.empty();
  }

  @Override
  public String getP(ConfigKey key, Object... formatArgs) {
    return get(ConfigKey.PREFIX) + get(key, formatArgs);
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
