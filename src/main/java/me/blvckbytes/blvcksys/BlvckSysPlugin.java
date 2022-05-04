package me.blvckbytes.blvcksys;

import me.blvckbytes.blvcksys.di.AutoConstructer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Main class of this plugin which bootstraps all other modules
*/
public class BlvckSysPlugin extends JavaPlugin {

  private static AutoConstructer ac;

  @Override
  public void onEnable() {
    try {
      // Create all resources within this package
      ac = new AutoConstructer(this);
      ac.execute();
    } catch (Exception e) {
      e.printStackTrace();
      // Disable this plugin if it didn't pass auto-construct
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    // Call cleanup on all interested resources
    if (ac != null)
      ac.cleanup();
  }
}
