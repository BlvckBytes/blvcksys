package me.blvckbytes.blvcksys;

import me.blvckbytes.blvcksys.util.di.AutoConstructer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

  @Override
  public void onEnable() {
    // Create all resources within the root package
    AutoConstructer.execute(this, "me.blvckbytes.blvcksys");

    CraftServer cs = ((CraftServer) getServer());
    MinecraftServer ms = (MinecraftServer) cs.getServer();
  }

  @Override
  public void onDisable() {
    // Call cleanup on all interested resources
    AutoConstructer.cleanup();
  }
}
