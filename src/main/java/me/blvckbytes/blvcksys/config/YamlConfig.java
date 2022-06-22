package me.blvckbytes.blvcksys.config;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Load a yaml configuration from it's file into memory and initialize all
  known config keys from the ConfigKey enum, then save to file again.
  Allows for quick access of Strings and Lists of Strings by using ConfigValue.
*/
@AutoConstruct
public class YamlConfig implements IConfig, IAutoConstructed {

  // Mapping config paths to a tuple of the in-memory config and it's underlying file
  private final Map<String, Tuple<YamlConfiguration, File>> configs;
  private final JavaPlugin plugin;

  @AutoInjectLate
  private IPlayerTextureHandler textureHandler;

  // Global prefix string, global palette, load ahead of time as it's used quite often
  private String prefix, palette;

  @AutoInjectLate
  private ILogger logger;

  public YamlConfig(
    @AutoInject JavaPlugin plugin
  ) {
    this.configs = new HashMap<>();
    this.plugin = plugin;

    // Build a fallback for the prefix
    this.prefix = "[" + plugin.getDescription().getName() + "] ";

    // Palette fallback: no colors
    this.palette = "";

    // Copy default config files from the resource folder
    this.copyDefaults("quests");

    // Initially load the main config (always needed)
    this.load("config");
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public ConfigValue get(ConfigKey key) {
    Tuple<YamlConfiguration, File> handle = configs.get("config");
    return retrieve(handle, key.toString()).orElse(ConfigValue.makeEmpty());
  }

  @Override
  public Optional<ConfigValue> get(String path, String key) {
    Tuple<YamlConfiguration, File> handle = load(path).orElse(null);
    return retrieve(handle, key);
  }

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
   * Retrieve a config value from a given handle by it's key
   * @param handle Config handle
   * @param key Key to retrieve
   * @return Optional value, empty if the handle was null or the key is invalid
   */
  @SuppressWarnings("unchecked")
  private Optional<ConfigValue> retrieve(Tuple<YamlConfiguration, File> handle, String key) {
    // Config failed to load
    if (handle == null)
      return Optional.empty();

    // Key unknown
    Object val = handle.a().get(key);
    if (val == null)
      return Optional.empty();

    // Is a list
    Class<?> valC = val.getClass();
    if (List.class.isAssignableFrom(valC))
      return Optional.of(new ConfigValue((List<Object>) val, prefix, palette));

    // Is a scalar
    else
      return Optional.of(new ConfigValue(val, prefix, palette));
  }

  /**
   * Load the config from the corresponding file
   * @param path Path to the file
   */
  private Optional<Tuple<YamlConfiguration, File>> load(String path) {
    Tuple<YamlConfiguration, File> handle = configs.get(path);

    // Config already loaded
    if (handle != null)
      return Optional.of(handle);

    try {
      File df = plugin.getDataFolder();

      // Create data folder if absent
      if (!df.exists())
        if (!df.mkdirs())
          throw new RuntimeException("Could not create data-folder to store the config in");

      File yf = new File(df.getAbsolutePath() + "/" + path + ".yml");

      // Create file if absent
      if (!yf.exists())
        if (!yf.createNewFile())
          throw new RuntimeException("Could not create config file");

      // Load configuration from file
      YamlConfiguration cfg = YamlConfiguration.loadConfiguration(yf);
      handle = new Tuple<>(cfg, yf);
      configs.put(path, handle);

      // Main configuration loaded
      if (path.equals("config")) {

        // Initialize config (appending missing keys) and only save on diffs
        if (ensureMainEntries(cfg))
          save(handle);

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
      }

      return Optional.of(handle);
    } catch (Exception e) {
      if (logger == null)
        e.printStackTrace();
      else
        logger.logError(e);
    }

    return Optional.empty();
  }

  /**
   * Initialize the yaml config with all values from the enum
   * @return True if items were added, false otherwise
   */
  private boolean ensureMainEntries(YamlConfiguration cfg) {
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
   * @param handle Handle to the file
   */
  private void save(Tuple<YamlConfiguration, File> handle) {
    // No config loaded yet
    if (handle.a() == null || handle.b() == null)
      return;

    try {
      // Save config using the file handle
      handle.a().save(handle.b());
    } catch (Exception e) {
      if (logger == null)
        e.printStackTrace();
      else
        logger.logError(e);
    }
  }

  /**
   * Copies default .yml files from the resources folder if they are not yet exising
   * @param folders Folders to copy from the resources-folder
   */
  private void copyDefaults(String... folders) {
    try {

      for (String folder : folders) {
        for (Tuple<String, InputStream> file : getResourceFiles(folder)) {
          // Not a yaml configuration file
          if (!file.a().endsWith(".yml"))
            continue;

          File f = new File(plugin.getDataFolder(), file.a());

          // This yaml has already been copied before
          if (f.exists())
            continue;

          // Create parent directories
          if (f.getParentFile().mkdirs()) {
            // Copy stream contents into the file
            FileOutputStream fos = new FileOutputStream(f);
            InputStream is = file.b();
            fos.write(is.readAllBytes());
            is.close();
            fos.close();
          }
        }
      }

    } catch (Exception e) {
      if (logger == null)
        e.printStackTrace();
      else
        logger.logError(e);
    }
  }

  /**
   * Gets all files of a folder within the jar's resources folder
   * @param directoryName Name of the target directory
   * @return List of tuple from file path to it's input stream containing the data
   */
  public List<Tuple<String, InputStream>> getResourceFiles(String directoryName) throws Exception {
    List<Tuple<String, InputStream>> streams = new ArrayList<>();
    URL url = getClass().getClassLoader().getResource(directoryName);

    // Resource not found
    if (url == null)
      return streams;

    // This routine only supports listing within the resources folder
    if (!url.getProtocol().equals("jar"))
      return streams;

    String dirname = directoryName + "/";
    String path = url.getPath();
    String jarPath = path.substring(5, path.indexOf("!"));

    try (
      JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name()))
    ) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();

        // Is a child of this directory
        if (name.startsWith(dirname) && !dirname.equals(name)) {
          InputStream resource = getClass().getClassLoader().getResourceAsStream(name);
          streams.add(new Tuple<>(name, resource));
        }
      }
    }

    return streams;
  }
}
