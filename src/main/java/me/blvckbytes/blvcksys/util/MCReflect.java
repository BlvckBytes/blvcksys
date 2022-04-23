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
  public static Optional<Field> findFieldByType(Class<?> c, String fieldClass) {
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
   * Try to find a class' member field's value by it's type, choose the first occurrence
   * @param o Object to search in
   * @param fieldClass Simple name of the target field's class
   * @return Optional field value, no value on reflection errors
   */
  public static Optional<Object> getFieldByType(Object o, String fieldClass) {
    try {
      // Try to get the field by it's type
      Optional<Field> f = findFieldByType(o.getClass(), fieldClass);

      if (f.isEmpty())
        return Optional.empty();

      // Respond with the value of this field in reference to the provided object
      f.get().setAccessible(true);
      return Optional.of(f.get().get(o));
    } catch (Exception e) {
      Main.logger().logError(e);
      return Optional.empty();
    }
  }

  /**
   * Try to set a class' member field's value by it's type, choose the first occurrence
   * @param o Object to manipulate in
   * @param fieldClass Simple name of the target field's class
   * @param v Value to set
   */
  public static void setFieldByType(Object o, String fieldClass, Object v) {
    findFieldByType(o.getClass(), fieldClass).ifPresent(f -> {
      try {
        f.setAccessible(true);
        f.set(o, v);
      } catch (Exception e) {
        Main.logger().logError(e);
      }
    });
  }

  /**
   * Checks whether or not the object is an instance of the required class
   * @param o Object to check
   * @param c Class to check for
   * @return True if it's an instance, false if not (or on errors)
   */
  public static boolean isInstanceOf(Object o, Class<?> c) {
    try {
      return c.isAssignableFrom(o.getClass());
    } catch (Exception e) {
      Main.logger().logError(e);
      return false;
    }
  }

  /**
   * Checks whether or not the object is an instance of the required NMS class
   * @param o Object to check
   * @param nmsClass Class to check for
   * @return True if it's an instance, false if not (or on errors)
   */
  public static boolean isInstanceOfNMS(Object o, String nmsClass) {
    try {
      Class<?> c = getClassNMS(nmsClass);
      return isInstanceOf(o, c);
    } catch (Exception e) {
      Main.logger().logError(e);
      return false;
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

  /**
   * Get a private field's value
   * @param o Object to modify
   * @param field Name of the field
   * @return Value of the field
   * @throws Exception Issues during reflection access
   */
  public static Optional<Object> getFieldByName(Object o, String field) {
    try {
      Class<?> cl = o.getClass();
      Field f = cl.getDeclaredField(field);
      f.setAccessible(true);
      return Optional.of(f.get(o));
    } catch (Exception e) {
      Main.logger().logError(e);
      return Optional.empty();
    }
  }

  /**
   * Set a private field's value
   * @param o Object to modify
   * @param field Name of the field
   * @param value New value
   */
  public static void setFieldByName(Object o, String field, Object value) {
    try {
      Class<?> cl = o.getClass();
      Field f = cl.getDeclaredField(field);
      f.setAccessible(true);
      f.set(o, value);
    } catch (Exception e) {
      Main.logger().logError(e);
    }
  }
}
