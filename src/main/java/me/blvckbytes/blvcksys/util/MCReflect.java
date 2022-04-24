package me.blvckbytes.blvcksys.util;

import io.netty.channel.Channel;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@AutoConstruct
public class MCReflect {

  private final JavaPlugin main;
  private final ILogger logger;
  private final String ver;

  public MCReflect(
    @AutoInject JavaPlugin main,
    @AutoInject ILogger logger
  ) {
    this.main = main;
    this.logger = logger;
    this.ver = findVersion();
  }

  /**
   * Find the minecraft version by parsing the bukkit package
   */
  private String findVersion() {
    return Bukkit.getServer().getClass().getName().split("\\.")[3];
  }

  /**
   * Get a class within the bukkit package
   * @param name Name of the class
   * @return Loaded class
   * @throws ClassNotFoundException Class could not be found
   */
  public Class<?> getClassBKT(String name) throws ClassNotFoundException {
    return Class.forName("org.bukkit.craftbukkit.%s.%s".formatted(ver, name));
  }

  /**
   * Get a class within the net-minecraft-server package
   * @param name Name of the class
   * @return Loaded class
   * @throws ClassNotFoundException Class could not be found
   */
  public Class<?> getClassNMS(String name) throws ClassNotFoundException {
    return Class.forName("net.minecraft.server.%s.%s".formatted(ver, name));
  }

  /**
   * Get the instance of the CraftServer
   */
  public Optional<Object> getCraftServer() {
    try {
      // Get the server instance and cast it to a CraftServer
      Class<?> clazz = getClassBKT("CraftServer");
      return Optional.of(clazz.cast(main.getServer()));
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
        Class<?> clazz = getClassBKT("CraftServer");
        Object cmdMap = clazz.getMethod("getCommandMap").invoke(cs);

        // Invoke the register method using the passed parameters
        cmdMap.getClass().getMethod("register", String.class, Command.class).invoke(cmdMap, name, command);
      } catch (Exception e) {
        logger.logError(e);
      }
    });
  }

  /**
   * Try to find a class' member field by it's type, choose the first occurrence
   * @param c Class to search in
   * @param fieldClass Simple name of the target field's class
   * @return Optional field, no value on reflection errors
   */
  public Optional<Field> findFieldByType(Class<?> c, String fieldClass) {
    try {
      Class<?> currC = c;
      Optional<Field> res = Optional.empty();

      while(
        // While there's still a superclass
        currC != null &&

        // And there's not yet a result
        res.isEmpty()
      ) {
        // Try to find the field
        res = Arrays.stream(currC.getDeclaredFields())
          .filter(it -> it.getType().getSimpleName().equals(fieldClass))
          .findFirst();

        // Walk into superclass
        currC = currC.getSuperclass();
      }

      return res;
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

  /**
   * Try to find a class' member field by it's name
   * @param c Class to search in
   * @param name Name of the target field
   * @return Optional field, no value on reflection errors
   */
  public Optional<Field> findFieldByName(Class<?> c, String name) {
    try {
      Class<?> currC = c;
      Optional<Field> res = Optional.empty();

      while(
        // While there's still a superclass
        currC != null &&

          // And there's not yet a result
          res.isEmpty()
      ) {
        // Try to find the field
        res = Arrays.stream(c.getDeclaredFields())
          .filter(it -> it.getName().equals(name))
          .findFirst();

        // Walk into superclass
        currC = currC.getSuperclass();
      }

      return res;
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

  /**
   * Try to find a class' member list field by it's generic type
   * @param c Class to search in
   * @param listType Name of the generic type
   * @return Optional field, no value on reflection errors
   */
  public Optional<Field> findListFieldByType(Class<?> c, String listType) {
    try {
      Class<?> currC = c;
      Optional<Field> res = Optional.empty();

      while(
        // While there's still a superclass
        currC != null &&

          // And there's not yet a result
          res.isEmpty()
      ) {
        // Try to find the field
        res = Arrays.stream(currC.getDeclaredFields())
          .filter(it -> List.class.isAssignableFrom(it.getType()))
          .filter(it -> it.getGenericType().getTypeName().contains(listType))
          .findFirst();

        // Walk into superclass
        currC = currC.getSuperclass();
      }

      return res;
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

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
   * Try to find a class' member field's value by it's type, choose the first occurrence
   * @param o Object to search in
   * @param fieldClass Simple name of the target field's class
   * @return Optional field value, no value on reflection errors
   */
  public Optional<Object> getFieldByType(Object o, String fieldClass) {
    try {
      // Try to get the field by it's type
      Optional<Field> f = findFieldByType(o.getClass(), fieldClass);

      if (f.isEmpty())
        return Optional.empty();

      // Respond with the value of this field in reference to the provided object
      f.get().setAccessible(true);
      return Optional.of(f.get().get(o));
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

  /**
   * Try to set a class' member field's value by it's type, choose the first occurrence
   * @param o Object to manipulate in
   * @param fieldClass Simple name of the target field's class
   * @param v Value to set
   */
  public void setFieldByType(Object o, String fieldClass, Object v) {
    findFieldByType(o.getClass(), fieldClass).ifPresent(f -> {
      try {
        f.setAccessible(true);
        f.set(o, v);
      } catch (Exception e) {
        logger.logError(e);
      }
    });
  }

  /**
   * Checks whether or not the object is an instance of the required class
   * @param o Object to check
   * @param c Class to check for
   * @return True if it's an instance, false if not (or on errors)
   */
  public boolean isInstanceOf(Object o, Class<?> c) {
    try {
      return c.isAssignableFrom(o.getClass());
    } catch (Exception e) {
      logger.logError(e);
      return false;
    }
  }

  /**
   * Checks whether or not the object is an instance of the required NMS class
   * @param o Object to check
   * @param nmsClass Class to check for
   * @return True if it's an instance, false if not (or on errors)
   */
  public boolean isInstanceOfNMS(Object o, String nmsClass) {
    try {
      Class<?> c = getClassNMS(nmsClass);
      return isInstanceOf(o, c);
    } catch (Exception e) {
      logger.logError(e);
      return false;
    }
  }

  /**
   * Get a player casted to a CraftPlayer
   * @param p Target Player
   * @return CraftPlayer instance
   */
  public Optional<Object> getCraftPlayer(Player p) {
    try {
      Class<?> cpC = getClassBKT("entity.CraftPlayer");
      return Optional.of(cpC.cast(p));
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
      return findFieldByType(epC, "PlayerConnection")
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
        findFieldByType(pc.getClass(), "NetworkManager")
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
   * Get a player's assigned NetworkChannel
   * @param p Target Player
   * @return NetworkChannel of the player
   */
  public Optional<Channel> getNetworkChannel(Player p) {
    return getNetworkManager(p)
      .flatMap(nm ->
        findFieldByType(nm.getClass(), "Channel")
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
   * Get a player's assigned NetworkChannel
   * @param nm NetworkManager instance to search for a channel in
   * @return NetworkChannel of the player
   */
  public Optional<Channel> getNetworkChannel(Object nm) {
    return findFieldByType(nm.getClass(), "Channel")
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

  /**
   * Set a private field's value
   * @param o Object to modify
   * @param field Name of the field
   * @param value New value
   */
  public void setFieldByName(Object o, String field, Object value) {
    try {
      Class<?> cl = o.getClass();
      Field f = cl.getDeclaredField(field);
      f.setAccessible(true);
      f.set(o, value);
    } catch (Exception e) {
      logger.logError(e);
    }
  }
}
