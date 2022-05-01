package me.blvckbytes.blvcksys.util;

import com.google.common.primitives.Primitives;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
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
  private final ILogger logger;
  private final String ver;

  public MCReflect(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger
  ) {
    this.plugin = plugin;
    this.logger = logger;
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
  public Optional<Class<?>> getClassBKT(String name) {
    try {
      return Optional.of(Class.forName("org.bukkit.craftbukkit.%s.%s".formatted(ver, name)));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Get the instance of the CraftServer
   */
  public Optional<Object> getCraftServer() {
    try {
      // Get the server instance and cast it to a CraftServer
      return getClassBKT("CraftServer").map(clazz -> clazz.cast(plugin.getServer()));
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

  /**
   * Register a command using the CraftServer's command map and thus
   * bypassing the tedious plugin.yml registrations
   * @param name Name of the command
   * @param command Command handler
   */
  public void registerCommand(String name, Command command) {
    getCraftServer().ifPresent(cs -> {
      try {
        // Get the server's command map
        getClassBKT("CraftServer")
          .flatMap(clazz -> findMethodByName(clazz, "getCommandMap"))
          .flatMap(m -> invokeMethod(m, cs))
          .ifPresent(cmdMap ->
            findMethodByName(cmdMap.getClass(), "register", String.class, Command.class)
              // Invoke the register method using the passed parameters
            .ifPresent(register -> invokeMethod(register, cmdMap, name, command))
          );
      } catch (Exception e) {
        logger.logError(e);
      }
    });
  }

  //=========================================================================//
  //                              Field By Type                              //
  //=========================================================================//

  /**
   * Try to find a class' member field by it's type, choose the first occurrence after skipping some
   * @param c Class to search in
   * @param fieldClass Target field's class
   * @param skip How many occurrences to skip
   * @return Optional field, no value on reflection errors
   */
  public Optional<Field> findFieldByType(Class<?> c, Class<?> fieldClass, int skip) {
    return walkHierarchyToFind(c, cc -> {
      try {
        return Arrays.stream(cc.getDeclaredFields())
          .filter(it -> !Modifier.isStatic(it.getModifiers()))
          .filter(it -> compareTypes(it.getType(), fieldClass, false))
          .skip(skip)
          .findFirst()
          .map(f -> {
            f.setAccessible(true);
            return f;
          });
      } catch (Exception e) {
        logger.logError(e);
        return Optional.empty();
      }
    });
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
   * @return Optional Object, no value on reflection errors
   */
  @SuppressWarnings("unchecked")
  public<C> Optional<C> getGenericFieldByType(Object o, Class<C> type, Class<?> genericType, int skip) {
    return walkHierarchyToFind(o.getClass(), cc -> {
      try {
        return (Optional<C>) findGenericFieldByType(cc, type, genericType, skip)
          .flatMap(f -> getFieldValue(f, o));
      } catch (Exception e) {
        logger.logError(e);
        return Optional.empty();
      }
    });
  }

  /**
   * Try to find a class' member field's value by it's type, choose the first occurrence
   * @param o Object to search in
   * @param fieldClass Target field's class
   * @param skip How many occurrences to skip
   * @return Optional field value, no value on reflection errors
   */
  @SuppressWarnings("unchecked")
  public<T> Optional<T> getFieldByType(Object o, Class<T> fieldClass, int skip) {
    try {
      // Try to get the field by it's type
      Optional<Field> f = findFieldByType(o.getClass(), fieldClass, skip);

      if (f.isEmpty())
        return Optional.empty();

      // Respond with the value of this field in reference to the provided object
      f.get().setAccessible(true);
      return Optional.of((T) f.get().get(o));
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

  /**
   * Try to find a class' member generic field by it's generic type
   * @param c Class to search in
   * @param type Type that holds the generic type
   * @param genericType Generic type held by type
   * @param skip How many occurrences to skip
   * @return Optional field, no value on reflection errors
   */
  public<T> Optional<Field> findGenericFieldByType(Class<?> c, Class<?> type, Class<T> genericType, int skip) {
    return walkHierarchyToFind(c, cc -> {
      try {
        return Arrays.stream(cc.getDeclaredFields())
          .filter(it -> !Modifier.isStatic(it.getModifiers()))
          .filter(it -> type.isAssignableFrom(it.getType()))
          .filter(it -> it.getGenericType().getTypeName().contains(genericType.getSimpleName()))
          .skip(skip)
          .findFirst();
      } catch (Exception e) {
        logger.logError(e);
        return Optional.empty();
      }
    });
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
    return findFieldByType(o.getClass(), fieldClass, skip).map(f -> {
      try {
        f.setAccessible(true);
        f.set(o, v);
        return true;
      } catch (Exception e) {
        logger.logError(e);
        return false;
      }
    }).orElse(false);
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
    return findGenericFieldByType(o.getClass(), c, type, skip).map(f -> {
      try {
        f.setAccessible(true);
        f.set(o, v);
        return true;
      } catch (Exception e) {
        logger.logError(e);
        return false;
      }
    }).orElse(false);
  }

  //=========================================================================//
  //                              Field By Name                              //
  //=========================================================================//

  /**
   * Try to find a class' member field by it's name
   * @param c Class to search in
   * @param name Name of the target field
   * @param skip How many occurrences to skip
   * @return Optional field, no value on reflection errors
   */
  public Optional<Field> findFieldByName(Class<?> c, String name, int skip) {
    return walkHierarchyToFind(c, cc -> {
      try {
        return Arrays.stream(cc.getDeclaredFields())
          .filter(it -> !Modifier.isStatic(it.getModifiers()))
          .filter(it -> it.getName().equals(name))
          .skip(skip)
          .findFirst()
          .map(f -> {
            f.setAccessible(true);
            return f;
          });
      } catch (Exception e) {
        logger.logError(e);
        return Optional.empty();
      }
    });
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
  public Optional<Object> getFieldByName(Object o, String field) {
    try {
      Class<?> cl = o.getClass();
      Field f = cl.getDeclaredField(field);
      f.setAccessible(true);
      return Optional.of(f.get(o));
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
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
    return setFieldByName(o, field, value, 0);
  }

  /**
   * Set a private field's value
   * @param o Object to modify
   * @param field Name of the field
   * @param value New value
   * @param skip How many occurrences to skip
   * @return Success state
   */
  public boolean setFieldByName(Object o, String field, Object value, int skip) {
    return findFieldByName(o.getClass(), field, skip)
      .map(f -> {
        try {
          f.set(o, value);
          return true;
        } catch (Exception e) {
          logger.logError(e);
          return false;
        }
      }).orElse(false);
  }

  //=========================================================================//
  //                             Field Resolver                              //
  //=========================================================================//

  /**
   * Try to get a field's value relative to an object
   * @param f Field to resolve
   * @param o Object to search in
   * @return Optional field value, no value on reflection errors
   */
  public Optional<Object> getFieldValue(Field f, Object o) {
    try {
      if (f == null)
        return Optional.empty();

      // Respond with the value of this field in reference to the provided object
      f.setAccessible(true);
      return Optional.of(f.get(o));
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

  /**
   * Try tosetget a field's value relative to an object
   * @param f Field to change
   * @param o Object to change in
   * @param v Value to set
   * @return Operation result
   */
  public boolean setFieldValue(Field f, Object o, Object v) {
    try {
      if (f == null)
        return false;

      f.setAccessible(true);
      f.set(o, v);
      return true;
    } catch (Exception e) {
      logger.logError(e);
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
  public<T extends Enum<T>> Optional<T> getEnumByField(Class<T> enumClass, Class<?> fieldClass, Object v, int skip) {
    try {
      // Find the target field inside the enum class
      Field f = findFieldByType(enumClass, fieldClass, skip).orElseThrow();

      // Loop all enum constants
      for (T eC : enumClass.getEnumConstants()) {
        // Check if the field value of this constant matches
        if (!f.get(eC).equals(v))
          continue;

        // Match
        return Optional.of(eC);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Could not find field
    return Optional.empty();
  }

  /**
   * Find an enum constant by it's numeric index
   * @param enumClass Class of the target enum
   * @param n Numeric index
   * @return Enum constant matching requirements
   */
  public<T extends Enum<T>> Optional<T> getEnumNth(Class<T> enumClass, int n) {
    try {
      return Arrays.stream(enumClass.getEnumConstants())
        .skip(n)
        .findFirst();
    } catch (Exception e) {
      logger.logError(e);
    }

    // Could not resolve the constant
    return Optional.empty();
  }

  //=========================================================================//
  //                                 Player                                  //
  //=========================================================================//

  /**
   * Get a player casted to a CraftPlayer
   * @param p Target Player
   * @return CraftPlayer instance
   */
  public Optional<Object> getCraftPlayer(Player p) {
    try {
      return getClassBKT("entity.CraftPlayer").map(cpC -> cpC.cast(p));
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

  /**
   * Get a player's assigned EntityPlayer
   * @param p Target Player
   * @return EntityPlayer of the player
   */
  public Optional<Object> getEntityPlayer(Player p) {
      return getCraftPlayer(p)
        .flatMap(cp -> {
          try {
            return Optional.of(cp.getClass().getMethod("getHandle").invoke(p));
          } catch (Exception e) {
            logger.logError(e);
            return Optional.empty();
          }
        });
  }

  /**
   * Get a player's assigned PlayerConnection
   * @param p Target Player
   * @return PlayerConnection of the player
   */
  public Optional<Object> getPlayerConnection(Player p) {
    return getEntityPlayer(p).flatMap(ep -> {
      Class<?> epC = ep.getClass();

      // Try to find a field of type PlayerConnection in the EntityPlayer
      return findFieldByType(epC, PlayerConnection.class, 0)
        .flatMap(field -> {
          try {
            return Optional.of(field.get(ep));
          } catch (Exception e) {
            logger.logError(e);
            return Optional.empty();
          }
        });
    });
  }

  /**
   * Get a player's assigned NetworkManager
   * @param p Target Player
   * @return NetworkManager of the player
   */
  public Optional<Object> getNetworkManager(Player p) {
    return getPlayerConnection(p)
      .flatMap(pc ->
        findFieldByType(pc.getClass(), NetworkManager.class, 0)
        .flatMap(nmf -> {
          try {
            return Optional.of(nmf.get(pc));
          } catch (Exception e) {
            logger.logError(e);
            return Optional.empty();
          }
        })
      );
  }

  /**
   * Send a packet to a specific player
   * @param p Player to send the packet to
   * @param packet Packet to send
   * @return Success state
   */
  public boolean sendPacket(Player p, Object packet) {
    return getNetworkManager(p)
      .flatMap(nm ->
        findMethodByArgsOnly(nm.getClass(), Packet.class)
          .map(sendM -> invokeMethod(sendM, nm, packet))
      ).isPresent();
  }

  /**
   * Get a player's assigned NetworkChannel
   * @param p Target Player
   * @return NetworkChannel of the player
   */
  public Optional<Channel> getNetworkChannel(Player p) {
    return getNetworkManager(p)
      .flatMap(nm ->
        findFieldByType(nm.getClass(), Channel.class, 0)
          .flatMap(cf -> {
            try {
              return Optional.of(cf.get(nm));
            } catch (Exception e) {
              logger.logError(e);
              return Optional.empty();
            }
          })
          .map(o -> ((Channel) o))
      );
  }

  /**
   * Get a NetworkManager's assigned NetworkChannel
   * @param nm NetworkManager instance to search for a channel in
   * @return NetworkChannel of the player
   */
  public Optional<Channel> getNetworkChannel(Object nm) {
    return findFieldByType(nm.getClass(), Channel.class, 0)
      .flatMap(cf -> {
        try {
          return Optional.of(cf.get(nm));
        } catch (Exception e) {
          logger.logError(e);
          return Optional.empty();
        }
      })
      .map(o -> (Channel) o);
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
  public Optional<Method> findMethodByName(Class<?> c, String name, Class<?> ...args) {
    return walkHierarchyToFind(c, (Class<?> cc) -> {
      try {
        return Optional.of(cc.getDeclaredMethod(name, args));
      } catch (Exception e) {
        return Optional.empty();
      }
    });
  }

  /**
   * Find a method only by it's argument types
   * @param c Class to search in
   * @param args Arg types of target method
   * @return Optional method
   */
  public Optional<Method> findMethodByArgsOnly(Class<?> c, Class<?> ...args) {
    return walkHierarchyToFind(c, (Class<?> cc) -> {
      try {
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
          if (matches)
            return Optional.of(m);
        }

        // Nothing matched
        return Optional.empty();
      } catch (Exception e) {
        logger.logError(e);
        return Optional.empty();
      }
    });
  }

  /**
   * Invoke a method safely using an internal try-catch
   * @param m Method to invoke
   * @param o Object to invoke on
   * @param args Arguments to that method
   * @return Method result
   */
  public Optional<Object> invokeMethod(Method m, Object o, Object ...args) {
    try {
      Object ret = m.invoke(o, args);

      if (ret == null)
        return Optional.empty();

      return Optional.of(ret);
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
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
  public Optional<Class<?>> findInnerClass(Class<?> container, String innerName) {
    try {
      return Optional.of(
        Class.forName(container.getName() + "$" + innerName)
      );
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

  /**
   * Walks the class hierarchy (superclasses) for as long as the searcher couldn't
   * come up with a result yet, or stops at the latest possible point end returns empty
   * @param c Class to walk
   * @param searcher Class member searcher funtion
   * @return Optional member
   * @param <T> Type of member to be searched
   */
  private<T> Optional<T> walkHierarchyToFind(Class<?> c, Function<Class<?>, Optional<T>> searcher) {
    try {
      Class<?> currC = c;
      Optional<T> res = Optional.empty();

      while(
        // While there's still a superclass
        currC != null &&

        // And there's not yet a result
        res.isEmpty()
      ) {
        // Try to find the target
        res = searcher.apply(currC);

        // Walk into superclass
        currC = currC.getSuperclass();
      }

      if (res.isEmpty())
        throw new RuntimeException("Hierarchy walk didn't yield any results (" + c + ")!");

      return res;
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
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
  public Optional<Object> invokeConstructor(Class<?> c, Object ...args) {
    try {
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
          return Optional.of(ctor.newInstance(args));
        }
      }

      // No matches found
      return Optional.empty();
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
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
  public Optional<Object> createPacket(Class<?> c) {
    return invokeConstructor(
      c, new PacketDataSerializer(Unpooled.wrappedBuffer(new byte[1024]))
    );
  }
}
