package me.blvckbytes.blvcksys;

import me.blvckbytes.blvcksys.util.di.AutoConstructer;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

  @Override
  public void onEnable() {
    // Create all resources within the root package
    AutoConstructer.execute(this, "me.blvckbytes.blvcksys");
  }

  @Override
  public void onDisable() {
    // Call cleanup on all interested resources
    AutoConstructer.cleanup();
  }
}
