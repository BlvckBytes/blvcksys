package me.blvckbytes.blvcksys.util;

import com.google.common.primitives.Primitives;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  A big collection of routines that handle reflective R/W access to
  all fields related to minecraft like networking, commands, enumerations,
  players and just the standard java class members.
*/
@AutoConstruct
public class MCReflect {

  private final JavaPlugin plugin;
  private final String ver;

  public MCReflect(
    @AutoInject JavaPlugin plugin
  ) {
    this.plugin = plugin;
    this.ver = findVersion();
  }

  //=========================================================================//
  //                            Version Handling                             //
  //=========================================================================//

  /**
   * Formats the playable version as a human readable string
   */
  public String getPlayableVersion() {
    return Arrays.stream(getVersion())
      .mapToObj(String::valueOf)
      .collect(Collectors.joining("."));
  }

  /**
   * Get the major, minor and revision version numbers the server's running on
   * @return [major, minor, revision]
   */
  public int[] getVersion() {
    String[] data = findVersion().split("_");
    return new int[] {
      Integer.parseInt(data[0].substring(1)), // remove leading v
      Integer.parseInt(data[1]),
      Integer.parseInt(data[2].substring(1)) // Remove leading R
    };
  }

  /**
   * Find the minecraft version by parsing the bukkit package
   * Format: v(MAJOR)_(MINOR)_R(REVISION)
   */
  private String findVersion() {
    return Bukkit.getServer().getClass().getName().split("\\.")[3];
  }

  /**
   * Get a class within the bukkit package
   * @param name Name of the class
   * @return Loaded class
   */
  public Class<?> getClassBKT(String name) throws ClassNotFoundException {
    return Class.forName("org.bukkit.craftbukkit.%s.%s".formatted(ver, name));
  }

  /**
   * Get the instance of the CraftServer
   */
  public Object getCraftServer() throws ClassNotFoundException {
    // Get the server instance and cast it to a CraftServer
    return getClassBKT("CraftServer").cast(plugin.getServer());
  }

  /**
   * Register a command using the CraftServer's command map and thus
   * bypassing the tedious plugin.yml registrations
   * @param name Name of the command
   * @param command Command handler
   * @return Success state
   */
  public void registerCommand(String name, Command command) throws Exception {
    Object cmdMap = invokeMethodByName(getCraftServer(), "getCommandMap", null);
    findMethodByName(cmdMap.getClass(), "register", String.class, Command.class).invoke(cmdMap, name, command);
  }

  //=========================================================================//
  //                              Field By Type                              //
  //=========================================================================//

  /**
   * Try to find a class' member field by it's type, choose the first occurrence after skipping some
   * @param c Class to search in
   * @param fieldClass Target field's class
   * @param skip How many occurrences to skip
   * @return Target field
   */
  public Field findFieldByType(Class<?> c, Class<?> fieldClass, int skip) throws Exception {
    return walkHierarchyToFind(c, cc ->
      Arrays.stream(cc.getDeclaredFields())
        .filter(it -> !Modifier.isStatic(it.getModifiers()))
        .filter(it -> compareTypes(it.getType(), fieldClass, false))
        .skip(skip)
        .findFirst()
        .map(f -> {
          f.setAccessible(true);
          return f;
        })
        .orElse(null)
    );
  }

  /**
   * Try to find a class' member field by it's type, choose the first occurrence after skipping some
   * @param o Object to use for the class to search in
   * @param fieldClass Target field's class
   * @param skip How many occurrences to skip
   * @return Target field
   */
  public Field findFieldByType(Object o, Class<?> fieldClass, int skip) throws Exception {
    return findFieldByType(o.getClass(), fieldClass, skip);
  }

  //=========================================================================//
  //                          Field Value By Type                            //
  //=========================================================================//

  ///////////////////////////////// Reading ////////////////////////////////////

  /**
   * Try to find a class' member generic field's value by it's generic type
   * @param o Object to search in
   * @param type Type that holds the generic type
   * @param genericType Generic type held by type
   * @param skip How many occurrences to skip
   * @return Target value
   */
  @SuppressWarnings("unchecked")
  public<C> C getGenericFieldByType(Object o, Class<C> type, Class<?> genericType, int skip) throws Exception {
    return walkHierarchyToFind(o.getClass(), cc -> (C) findGenericFieldByType(cc, type, genericType, skip).get(o));
  }

  /**
   * Try to find a class' member field's value by it's type, choose the first occurrence
   * @param o Object to search in
   * @param fieldClass Target field's class
   * @param skip How many occurrences to skip
   * @return Target value
   */
  @SuppressWarnings("unchecked")
  public<T> T getFieldByType(Object o, Class<T> fieldClass, int skip) throws Exception {
    return (T) findFieldByType(o.getClass(), fieldClass, skip).get(o);
  }

  /**
   * Try to find a class' member generic field by it's generic type
   * @param c Class to search in
   * @param type Type that holds the generic type
   * @param genericType Generic type held by type
   * @param skip How many occurrences to skip
   * @return Target field
   */
  public<T> Field findGenericFieldByType(Class<?> c, Class<?> type, Class<T> genericType, int skip) throws Exception {
    return walkHierarchyToFind(c, cc ->
      Arrays.stream(cc.getDeclaredFields())
        .filter(it -> !Modifier.isStatic(it.getModifiers()))
        .filter(it -> type.isAssignableFrom(it.getType()))
        .filter(it -> it.getGenericType().getTypeName().contains(genericType.getSimpleName()))
        .skip(skip)
        .findFirst()
        .map(f -> {
          f.setAccessible(true);
          return f;
        })
        .orElse(null)
    );
  }

  ////////////////////////////////// Writing /////////////////////////////////////

  /**
   * Try to set a class' member field's value by it's type, choose the first occurrence
   * @param o Object to manipulate in
   * @param fieldClass Target field's class
   * @param v Value to set
   * @param skip How many occurrences to skip
   * @return Success state
   */
  public boolean setFieldByType(Object o, Class<?> fieldClass, Object v, int skip) {
    try {
      findFieldByType(o.getClass(), fieldClass, skip).set(o, v);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Try to set a class' member field's value by it's type, choose the first occurrence
   * @param o Object to manipulate in
   * @param c Class to search in
   * @param type Type that holds the generic type
   * @param v Value to set
   * @param skip How many occurrences to skip
   * @return Success state
   */
  public boolean setGenericFieldByType(Object o, Class<?> c, Class<?> type, Object v, int skip) {
    try {
      findGenericFieldByType(o.getClass(), c, type, skip).set(o, v);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  //=========================================================================//
  //                              Field By Name                              //
  //=========================================================================//

  /**
   * Try to find a class' member field by it's name
   * @param c Class to search in
   * @param name Name of the target field
   * @return Optional field, no value on reflection errors
   */
  public Field findFieldByName(Class<?> c, String name) throws Exception {
    return walkHierarchyToFind(c, cc ->
      Arrays.stream(cc.getDeclaredFields())
        .filter(it -> !Modifier.isStatic(it.getModifiers()))
        .filter(it -> it.getName().equals(name))
        .findFirst()
        .map(f -> {
          f.setAccessible(true);
          return f;
        })
        .orElse(null)
    );
  }

  //=========================================================================//
  //                           Field Value By Name                           //
  //=========================================================================//

  ///////////////////////////////// Reading ////////////////////////////////////

  /**
   * Get a private field's value
   * @param o Object to modify
   * @param field Name of the field
   * @return Value of the field
   */
  public Object getFieldByName(Object o, String field) throws Exception {
    Class<?> cl = o.getClass();
    Field f = cl.getDeclaredField(field);
    f.setAccessible(true);
    return f.get(o);
  }

  ////////////////////////////////// Writing /////////////////////////////////////

  /**
   * Set a private field's value
   * @param o Object to modify
   * @param field Name of the field
   * @param value New value
   * @return Success state
   */
  public boolean setFieldByName(Object o, String field, Object value) {
    try {
      findFieldByName(o.getClass(), field).set(o, value);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  //=========================================================================//
  //                              Enumerations                               //
  //=========================================================================//

  /**
   * Find an enum constant by a value of it's fields
   * @param enumClass Class of the target enum
   * @param fieldClass Class of the target field within that enum's class
   * @param v Value the field needs to equal to
   * @param skip Number if fields to skip of that type
   * @return Enum constant matching requirements
   */
  public<T extends Enum<T>> T getEnumByField(Class<T> enumClass, Class<?> fieldClass, Object v, int skip) throws Exception {
    // Find the target field inside the enum class
    Field f = findFieldByType(enumClass, fieldClass, skip);

    // Loop all enum constants
    for (T eC : enumClass.getEnumConstants()) {
      // Check if the field value of this constant matches
      if (!f.get(eC).equals(v))
        continue;

      // Match
      return eC;
    }

    // Nothing found
    return null;
  }

  /**
   * Find an enum constant by it's numeric index
   * @param enumClass Class of the target enum
   * @param n Numeric index
   * @return Enum constant matching requirements
   */
  public<T extends Enum<T>> T getEnumNth(Class<T> enumClass, int n) throws Exception {
    return Arrays.stream(enumClass.getEnumConstants())
      .skip(n)
      .findFirst()
      .orElseThrow();
  }

  //=========================================================================//
  //                                 Player                                  //
  //=========================================================================//

  /**
   * Get a player casted to a CraftPlayer
   * @param p Target Player
   * @return CraftPlayer instance
   */
  public Object getCraftPlayer(Player p) throws Exception {
    return getClassBKT("entity.CraftPlayer").cast(p);
  }

  /**
   * Get a player's assigned EntityPlayer
   * @param p Target Player
   * @return EntityPlayer of the player
   */
  public Object getEntityPlayer(Player p) throws Exception {
    Object cp = getCraftPlayer(p);
    return invokeMethodByName(cp, "getHandle", null);
  }

  /**
   * Get a player's assigned PlayerConnection
   * @param p Target Player
   * @return PlayerConnection of the player
   */
  public Object getPlayerConnection(Player p) throws Exception {
    Object ep = getEntityPlayer(p);
    return findFieldByType(ep, PlayerConnection.class, 0).get(ep);
  }

  /**
   * Get a player's assigned NetworkManager
   * @param p Target Player
   * @return NetworkManager of the player
   */
  public Object getNetworkManager(Player p) throws Exception {
    Object pc = getPlayerConnection(p);
    return findFieldByType(pc, NetworkManager.class, 0).get(pc);
  }

  /**
   * Send a packet to a specific player
   * @param p Player to send the packet to
   * @param packet Packet to send
   * @return Success state
   */
  public boolean sendPacket(Player p, Object packet) {
    try {
      Object nm = getNetworkManager(p);
      findMethodByArgsOnly(nm, Packet.class).invoke(nm, packet);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Get a player's assigned NetworkChannel
   * @param p Target Player
   * @return NetworkChannel of the player
   */
  public Channel getNetworkChannel(Player p) throws Exception {
    Object nm = getNetworkManager(p);
    return getFieldByType(nm, Channel.class, 0);
  }

  /**
   * Get a NetworkManager's assigned NetworkChannel
   * @param nm NetworkManager instance to search for a channel in
   * @return NetworkChannel of the player
   */
  public Channel getNetworkChannel(Object nm) throws Exception {
    return getFieldByType(nm, Channel.class, 0);
  }

  //=========================================================================//
  //                                 Methods                                 //
  //=========================================================================//

  /**
   * Find a method by it's name and it's argument types
   * @param c Class to search in
   * @param name Name of the method
   * @param args Arg types of target method
   * @return Optional method
   */
  public Method findMethodByName(Class<?> c, String name, Class<?> ...args) throws Exception {
    return walkHierarchyToFind(c, (Class<?> cc) -> {
      try {
        Method m = cc.getDeclaredMethod(name, args);
        m.setAccessible(true);
        return m;
      } catch (Exception e) {
        return null;
      }
    });
  }

  public Object invokeMethodByName(Object o, String name, @Nullable Class<?>[] args, Object... values) throws Exception {
    return findMethodByName(o.getClass(), name, args).invoke(o, values);
  }

  public Object invokeMethodByArgsOnly(Object o, @Nullable Class<?>[] args, Object... values) throws Exception {
    return findMethodByArgsOnly(o.getClass(), args).invoke(o, values);
  }

  /**
   * Find a method only by it's argument types
   * @param o Object to use for the class to search in
   * @param args Arg types of target method
   * @return Optional method
   */
  public Method findMethodByArgsOnly(Object o, Class<?> ...args) throws Exception {
    return findMethodByArgsOnly(o.getClass(), args);
  }

  /**
   * Find a method only by it's argument types
   * @param c Class to search in
   * @param args Arg types of target method
   * @return Optional method
   */
  public Method findMethodByArgsOnly(Class<?> c, Class<?> ...args) throws Exception {
    return walkHierarchyToFind(c, (Class<?> cc) -> {
      for (Method m : cc.getDeclaredMethods()) {
        Class<?>[] paramTypes = m.getParameterTypes();

        // Parameter count mismatch
        if (paramTypes.length != args.length)
          continue;

        // Compare all args individually
        boolean matches = true;
        for (int i = 0; i < paramTypes.length; i++) {
          // Type matches
          if (compareTypes(paramTypes[i], args[i], false))
            continue;

          // Parameter mismatch
          matches = false;
          break;
        }

        // Args match, return this method
        if (matches) {
          m.setAccessible(true);
          return m;
        }
      }

      // Nothing matched
      return null;
    });
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Finds an inner class (hidden inside another class) by it's name
   * @param container Containing class
   * @param innerName Inner class' name
   * @return Optional class
   */
  public Class<?> findInnerClass(Class<?> container, String innerName) throws Exception {
    return Class.forName(container.getName() + "$" + innerName);
  }

  /**
   * Walks the class hierarchy (superclasses) for as long as the searcher couldn't
   * come up with a result yet, or stops at the latest possible point end returns empty
   * @param c Class to walk
   * @param searcher Class member searcher funtion
   * @return Optional member
   * @param <T> Type of member to be searched
   */
  private<T> T walkHierarchyToFind(Class<?> c, UnsafeFunction<Class<?>, T> searcher) throws Exception {
    Class<?> currC = c;
    T res = null;

    while(
      // While there's still a superclass
      currC != null &&

      // And there's not yet a result
      res == null
    ) {
      // Try to find the target
      res = searcher.apply(currC);

      // Walk into superclass
      currC = currC.getSuperclass();
    }

    if (res == null)
      throw new RuntimeException("Hierarchy walk didn't yield any results (" + c + ")!");

    return res;
  }

  /**
   * Compare types and account for primitive wrappers
   * @param a Class a
   * @param b Class b
   * @param acceptAssignable Whether or not to accept isAssignableFrom as equality
   * @return True if a equals b, false otherwise
   */
  private boolean compareTypes(Class<?> a, Class<?> b, boolean acceptAssignable) {
    // Unwrap a if it's a wrapper
    if (Primitives.isWrapperType(a))
      a = Primitives.unwrap(a);

    // Unwrap b if it's a wrapper
    if (Primitives.isWrapperType(b))
      b = Primitives.unwrap(b);

    // Compare
    return (
      // A directly equals B
      a.equals(b) ||
      // Assignable is accepted, compare on that level
      (acceptAssignable && (a.isAssignableFrom(b) || b.isAssignableFrom(a)))
    );
  }

  /**
   * Invoke a class' constructor, no matter if it's accessible or not
   * @param c Class to search for constructors in
   * @param args Arguments to invoke with
   * @return Optional instance
   */
  public Object invokeConstructor(Class<?> c, Object ...args) throws Exception {
    // Search through all constructors
    for (Constructor<?> ctor : c.getDeclaredConstructors()) {
      Parameter[] params = ctor.getParameters();

      // Arg count mismatch
      if (params.length != args.length)
        continue;

      // Check args
      boolean matches = true;
      for (int i = 0; i < params.length; i++) {
        if (compareTypes(params[i].getType(), args[i].getClass(), true))
          continue;

        // Arg mismatch
        matches = false;
        break;
      }

      // Return instance using matching constructor
      if (matches) {
        ctor.setAccessible(true);
        return ctor.newInstance(args);
      }
    }

    // No matches found
    throw new IllegalStateException("Could not find a constructor!");
  }

  //=========================================================================//
  //                                 Packets                                 //
  //=========================================================================//

  /**
   * Create a new instance of a packet containing "zero-ed" data
   * to be overwritten later on. This is handy as packets don't have
   * an empty default constructor.
   * @param c Class of the target to create
   * @return Optional instance, empty when the packet doesn't support this method
   */
  public Object createPacket(Class<?> c) throws Exception {
    return invokeConstructor(c, new PacketDataSerializer(Unpooled.wrappedBuffer(new byte[1024])));
  }
}
