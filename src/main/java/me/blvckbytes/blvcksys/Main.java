package me.blvckbytes.blvcksys;

import me.blvckbytes.blvcksys.config.Config;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.di.AutoConstructer;
import me.blvckbytes.blvcksys.util.logging.ConsoleSenderLogger;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

  private static ILogger logger;
  private static Main inst;

  @Override
  public void onEnable() {
    inst = this;

    // Load the config file
    try {
      Config.load(this);
    } catch (Exception e) {
      logger().logError(e);
    }

    // Auto-construct all commands, which loads them through their super-calls
    AutoConstructer.execute("me.blvckbytes.blvcksys.commands");

    logger().logInfo("Plugin loaded successfully!");
  }

  @Override
  public void onDisable() {
    // Call cleanup on our resources
    AutoConstructer.cleanup();

    logger().logInfo("Plugin unloaded successfully!");
  }

  public static ILogger logger() {
    // Create logger on the first call
    // Calls should only appear after onEnable, and thus we can expect Config to be loaded
    if (logger == null)
      // All logging occurs within the console
      logger = new ConsoleSenderLogger(Config.get(ConfigKey.PREFIX), true);

    return logger;
  }
  public static Main getInst() { return inst; }
}
