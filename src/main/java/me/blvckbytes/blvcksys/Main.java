package me.blvckbytes.blvcksys;

import me.blvckbytes.blvcksys.util.di.AutoConstructer;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

  @Override
  public void onEnable() {
    // Create all resources within this package
    AutoConstructer.execute(this, getClass().getPackageName());
  }

  @Override
  public void onDisable() {
    // Call cleanup on all interested resources
    AutoConstructer.cleanup();
  }
}
