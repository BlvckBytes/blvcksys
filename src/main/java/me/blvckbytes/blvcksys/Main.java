package me.blvckbytes.blvcksys;

import me.blvckbytes.blvcksys.config.Config;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.di.AutoConstructer;
import me.blvckbytes.blvcksys.util.logging.ConsoleSenderLogger;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

  // All logging occurs within the console
  private static final ILogger logger = new ConsoleSenderLogger("BVS", true);

  private static Main inst;

  @Override
  public void onEnable() {
    inst = this;
    // Auto-construct all commands, which loads them through their super-calls
    AutoConstructer.execute("me.blvckbytes.blvcksys.commands");

    // Load the config file
    try {
      Config.load(this);
    } catch (Exception e) {
      logger().logError(e);
    }

    logger().logInfo("Plugin loaded successfully!");
  }

  @Override
  public void onDisable() {
    logger().logInfo("Plugin unloaded successfully!");
  }

  public static ILogger logger() {
    return logger;
  }
  public static Main getInst() { return inst; }
}
