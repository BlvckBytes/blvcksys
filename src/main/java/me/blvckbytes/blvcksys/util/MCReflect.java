package me.blvckbytes.blvcksys.util;

import io.netty.channel.Channel;
import me.blvckbytes.blvcksys.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

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

  /**
   * Try to find a class' member field by it's type, choose the first occurrence
   * @param c Class to search in
   * @param fieldClass Simple name of the target field's class
   * @return Optional field, no value on reflection errors
   */
  private static Optional<Field> findFieldByType(Class<?> c, String fieldClass) {
    try {
      // Try to find a field of type PlayerConnection in the EntityPlayer
      return Arrays.stream(c.getDeclaredFields())
        .filter(it -> it.getType().getSimpleName().equals(fieldClass))
        .findFirst();
    } catch (Exception e) {
      Main.logger().logError(e);
      return Optional.empty();
    }
  }

  /**
   * Get a player casted to a CraftPlayer
   * @param p Target Player
   * @return CraftPlayer instance
   * @throws Exception Issues during reflection access
   */
  public static Object getCraftPlayer(Player p) throws Exception {
    Class<?> cpC = getClassBKT("entity.CraftPlayer");
    return cpC.cast(p);
  }

  /**
   * Get a player's assigned EntityPlayer
   * @param p Target Player
   * @return EntityPlayer of the player
   * @throws Exception Issues during reflection access
   */
  public static Object getEntityPlayer(Player p) throws Exception {
    Object cp = getCraftPlayer(p);
    return cp.getClass().getMethod("getHandle").invoke(p);
  }

  /**
   * Get a player's assigned PlayerConnection
   * @param p Target Player
   * @return PlayerConnection of the player
   * @throws Exception Issues during reflection access
   */
  public static Object getPlayerConnection(Player p) throws Exception {
    Object ep = getEntityPlayer(p);
    assert ep != null;

    Class<?> epC = ep.getClass();

    // Try to find a field of type PlayerConnection in the EntityPlayer
    Optional<Field> field = findFieldByType(epC, "PlayerConnection");

    // Field not found
    if (field.isEmpty())
      throw new RuntimeException("Could not find a field of type PlayerConnection!");

    return field.get().get(ep);
  }

  /**
   * Get a player's assigned NetworkManager
   * @param p Target Player
   * @return NetworkManager of the player
   * @throws Exception Issues during reflection access
   */
  public static Object getNetworkManager(Player p) throws Exception {
    Object pc = getPlayerConnection(p);
    assert pc != null;

    Class<?> pcC = pc.getClass();

    // Try to find a field of type NetworkManager in the EntityPlayer
    Optional<Field> field = findFieldByType(pcC, "NetworkManager");

    // Field not found
    if (field.isEmpty())
      throw new RuntimeException("Could not find a field of type NetworkManager!");

    return field.get().get(pc);
  }

  /**
   * Get a player's assigned NetworkChannel
   * @param p Target Player
   * @return NetworkChannel of the player
   * @throws Exception Issues during reflection access
   */
  public static Channel getNetworkChannel(Player p) throws Exception {
    Object nm = getNetworkManager(p);
    assert nm != null;

    Class<?> nmC = nm.getClass();

    // Try to find a field of type NetworkManager in the EntityPlayer
    Optional<Field> field = findFieldByType(nmC, "Channel");

    // Field not found
    if (field.isEmpty())
      throw new RuntimeException("Could not find a field of type Channel!");

    return (Channel) field.get().get(nm);
  }
}
