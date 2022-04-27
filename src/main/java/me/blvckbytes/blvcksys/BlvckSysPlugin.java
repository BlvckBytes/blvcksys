package me.blvckbytes.blvcksys;

import me.blvckbytes.blvcksys.util.di.AutoConstructer;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Main class of this plugin which bootstraps all other modules
*/
public class BlvckSysPlugin extends JavaPlugin {

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
