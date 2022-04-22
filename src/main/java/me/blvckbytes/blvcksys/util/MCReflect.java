package me.blvckbytes.blvcksys.util;

import me.blvckbytes.blvcksys.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;

public class MCReflect {

  private static String ver = findVersion();

  /**
   * Find the minecraft version by parsing the bukkit package
   */
  private static String findVersion() {
    return Bukkit.getServer().getClass().getName().split("\\.")[3];
  }

  /**
   * Get a class within the bukkit package
   * @param name Name of the class
   * @return Loaded class
   * @throws ClassNotFoundException Class could not be found
   */
  public static Class<?> getClassBKT(String name) throws ClassNotFoundException {
    return Class.forName("org.bukkit.craftbukkit.%s.%s".formatted(ver, name));
  }

  /**
   * Get a class within the net-minecraft-server package
   * @param name Name of the class
   * @return Loaded class
   * @throws ClassNotFoundException Class could not be found
   */
  public static Class<?> getClassNMS(String name) throws ClassNotFoundException {
    return Class.forName("net.minecraft.server.%s.%s".formatted(ver, name));
  }

  /**
   * Get the instance of the CraftServer
   */
  public static Object getCraftServer() throws ClassNotFoundException {
    // Get the server instance and cast it to a CraftServer
    Class<?> clazz = getClassBKT("CraftServer");
    return clazz.cast(Main.getInst().getServer());
  }

  /**
   * Register a command using the CraftServer's command map and thus
   * bypassing the tedious plugin.yml registrations
   * @param name Name of the command
   * @param command Command handler
   */
  public static void registerCommand(String name, Command command) {
    try {
      Object cs = getCraftServer();

      // Get the server's command map
      Class<?> clazz = getClassBKT("CraftServer");
      Object cmdMap = clazz.getMethod("getCommandMap").invoke(cs);

      // Invoke the register method using the passed parameters
      cmdMap.getClass().getMethod("register", String.class, Command.class).invoke(cmdMap, name, command);
    } catch (Exception e) {
      Main.logger().logError(e);
    }
  }
}
