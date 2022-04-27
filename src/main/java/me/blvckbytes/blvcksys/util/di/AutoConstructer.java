package me.blvckbytes.blvcksys.util.di;

import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Scans all children of a package for classes which are annotated by the
  target annotation to be auto-constructed. Those classes may have multiple
  dependencies within their constructor, so the constructer generates a
  dependency graph and resolves it through creating one dependency after the other.
  Dependencies can be concrete classes or interfaces, where in the later case
  only one class is allowed to implement this interface within the package, an
  exception will be generated otherwise. In order to break up circular
  dependencies, injected dependencies can be marked as lateinit. A lateinit
  parameter always needs to have a corresponding class member field of the
  same type, into which the value gets injected as soon as it's available,
  where the constructor will be passed a null-value for it in the mean time.
*/
public class AutoConstructer {

  // Cache for already constructed classes (singletons)
  private static final Map<Class<?>, Object> refs;

  // Cache for @AutoConstruct'ed class fields that are waiting for late init injections
  private static final Map<Class<?>, List<Tuple<Object, Field>>> lateinits;

  // Queues log messages until the logger is available
  private static final List<String> logQueue;

  static {
    refs = new HashMap<>();
    lateinits = new HashMap<>();
    logQueue = new ArrayList<>();
  }

  /**
   * Calls the cleanup-routine on all resources that implement it
   */
  public static void cleanup() {
    // Iterate all created instances
    for (Object ref : refs.values()) {
      // Does not implement the autoconstructed interface (which is not mandatory), thus skip
      if (!IAutoConstructed.class.isAssignableFrom(ref.getClass()))
        continue;

      // Call cleanup
      ((IAutoConstructed) ref).cleanup();
    }
  }

  /**
   * Find all classes within the provided package that make use of {@link AutoConstruct}
   *
   * @param plugin JavaPlugin reference
   * @param pkg Package to search for targets in
   */
  private static List<Class<?>> findAnnotatedClasses(JavaPlugin plugin, String pkg) {
    List<Class<?>> classes = new ArrayList<>();

    try {
      // Get the executing jar's file path
      String fpath = new File(
        plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
      ).getPath();

      // Transform the default package notation into a path
      String pathPkg = pkg.replace(".", "/");

      // Load the jar file by it's path
      JarFile jf = new JarFile(fpath);

      // Loop all of it's entries (packages, classes, files, ...)
      jf.entries().asIterator().forEachRemaining(je -> {
        String name = je.getName();

        // Not a class within the target package
        if (!(name.startsWith(pathPkg) && name.endsWith(".class")))
          return;

        // Try loading the class (should succeed every time) and add it to the local list
        try {
          classes.add(Class.forName(
            // Transform the path back into package notation and strip off .class
            name.substring(0, name.lastIndexOf('.')).replace("/", ".")
          ));
        } catch (Exception e) {
          e.printStackTrace();
        }
      });

      jf.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Only return classes that have the matching annotation applied
    return classes
      .stream()
      .filter(c -> c.isAnnotationPresent(AutoConstruct.class))
      .toList();
  }

  /**
   * Execute the auto-constructor and thus instantiate all available
   * classes within the specified package that are annotated by @AutoConstruct
   *
   * @param plugin JavaPlugin reference
   * @param pkg Package to search for targets in
   */
  public static void execute(JavaPlugin plugin, String pkg) {
    try {
      // Find all classes in the target package
      List<Class<?>> classes = findAnnotatedClasses(plugin, pkg);

      // Mapping classes to a chosen constructor, which either has no deps or only @AutoInject dep parameters
      Map<Class<?>, Constructor<?>> ctorMap = selectConstructors(classes);

      // Resolve all dependencies recursively
      List<Class<?>> seen = new ArrayList<>();
      for (Map.Entry<Class<?>, Constructor<?>> e : ctorMap.entrySet())
        createWithDependencies(plugin, ctorMap, e.getKey(), seen);

      // Call the init method on all resources
      for (Object o : refs.values()) {
        if (o instanceof IAutoConstructed a)
          a.initialize();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * If the target is an interface, this routine tries to resolve it to an actual
   * implementing class to be worked with further on. Multiple or no implementations
   * will result in a RuntimeError
   *
   * @param target  Target class to resolve
   * @param classes List of available classes to choose from
   * @return Resolved class for interfaces, target for implementations
   */
  private static Class<?> resolveInterface(Class<?> target, List<? extends Class<?>> classes) {
    // Not an interface, just return the implementation itself
    if (!target.isInterface())
      return target;

    // This dependency is an interface, search for a class that implements it
    Class<?> impl = null;

    // Search through all classes
    for (Class<?> pi : classes) {
      // Skip self
      if (pi == target)
        continue;

      // Not implementing this interface
      if (!target.isAssignableFrom(pi))
        continue;

      // There should never be multiple implementations present (for safety)
      // To disable another implementation, just remove it's @AutoConstruct annotation
      if (impl != null)
        throw new RuntimeException("Multiple Implementations for @AutoInject found: %s".formatted(target.getName()));

      // Use the first occurring implementation
      impl = pi;
    }

    // This interface never got implemented (or just not @AutoConstruct'ed)
    if (impl == null)
      throw new RuntimeException("No @AutoInject implementation found: %s".formatted(target.getName()));

    // Now continue with the actual implementation
    return impl;
  }

  /**
   * Transform a list of available classes into a map which assigns them to their
   * selected and valid constructor to be used later down the road. If a class has no
   * usable constructor, a RuntimeError will be thrown
   *
   * @param classes List of available classes
   * @return Map of class to valid constructor
   */
  private static Map<Class<?>, Constructor<?>> selectConstructors(List<? extends Class<?>> classes) {
    Map<Class<?>, Constructor<?>> ctorMap = new HashMap<>();

    // Loop all classes
    for (Class<?> c : classes) {

      // Loop all constructors of this class
      Constructor<?> tarCtor = null;
      for (Constructor<?> ctor : c.getDeclaredConstructors()) {

        // Constructor is private
        // They need to be skipped as it would otherwise be hard to distinguish between
        // use intended and non-external-use intended constructors
        if (!ctor.canAccess(null))
          continue;

        // Whether or not this constructor is usable, which either means
        // that it only has @AutoInject parameters, or no parameters
        boolean isUsable = true;

        // Loop all parameters of this constructor
        for (Parameter p : ctor.getParameters()) {
          // Parameter without auto-inject annotation, cannot be resolved
          if (!p.isAnnotationPresent(AutoInject.class)) {
            isUsable = false;
            break;
          }
        }

        // Skip
        if (!isUsable)
          continue;

        // Use this constructor
        tarCtor = ctor;
        break;
      }

      // Cannot construct this class
      if (tarCtor == null)
        throw new RuntimeException("No valid @AutoConstruct constructors in " + c.getName());

      // Register this constructor
      ctorMap.put(c, tarCtor);
    }

    return ctorMap;
  }

  /**
   * Called whenever a resource has been instantiated
   * @param plugin Reference of the JavaPlugin instance
   * @param instance Created object
   * @param vanillaC Vanilla class (unresolved interface for example) of this object
   */
  private static void onInstantiation(JavaPlugin plugin, Object instance, Class<?> vanillaC) {
    String name = instance.getClass().getSimpleName();
    logDebug("@AutoConstruct: " + name);

    // Check for lateinits that need this type
    List<Tuple<Object, Field>> receivers = lateinits.remove(vanillaC);
    if (receivers != null) {
      try {
        // Loop all tuples of object to field and set the value
        for (Tuple<Object, Field> fr : receivers) {
          Field f = fr.b();
          f.setAccessible(true);
          f.set(fr.a(), instance);

          logDebug("Lateinit " + name + " (" + fr.a().getClass().getSimpleName() + ")");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // Also register events if the listener interface has been implemented
    if (instance instanceof Listener l) {
      plugin.getServer().getPluginManager().registerEvents(l, plugin);
      logDebug("EventListener: " + name);
    }
  }

  /**
   * Create a new instance of a class by creating all it's constructor's dependencies beforehand
   * @param plugin Reference of the JavaPlugin instance
   * @param ctorMap Constructor map of pre-selected, valid constructors
   * @param target Target class to construct
   * @param seen List of already seen classes, passed for recursion
   * @return Instantiated object ref
   *
   * @throws Exception Issues with instantiation or dependency conflicts
   */
  private static Object createWithDependencies(
    JavaPlugin plugin,
    Map<Class<?>, Constructor<?>> ctorMap,
    Class<?> target,
    List<Class<?>> seen
  ) throws Exception {
    // Resolve the interface implementation, if applicable
    Class<?> vanillaC = target;
    target = resolveInterface(target, new ArrayList<>(ctorMap.keySet()));

    // Already exists, return "singleton" object
    if (refs.containsKey(target))
      return refs.get(target);

    // Check for unknown dependencies
    Constructor<?> targetC = ctorMap.get(target);
    if (targetC == null)
      throw new RuntimeException("Could not find @AutoInject dependency: %s".formatted(target.getName()));

    // Encountered a leaf node - no more dependencies to resolve
    Parameter[] params = targetC.getParameters();
    if (params.length == 0) {
      // Invoke empty constructor
      Object inst = targetC.newInstance();
      onInstantiation(plugin, inst, vanillaC);
      refs.put(target, inst);

      // As this dependency now exists, remove it from the seen list, as it
      // cannot cause any further circular dependencies
      seen.remove(target);
      return inst;
    }

    // Keep a list of fields within this class that need to be late-initialized
    List<Field> lateinitFields = new ArrayList<>();

    // Loop all parameters of this constructor and thus all dependencies
    Object[] args = new Object[params.length];
    for (int i = 0; i < params.length; i++) {
      Parameter p = params[i];
      Class<?> dep = p.getType();

      // Get the auto-inject annotation to access it's parameters
      AutoInject aia = p.getAnnotation(AutoInject.class);
      if (aia == null)
        throw new RuntimeException(
          "Parameter %s of %s is not annotated by @AutoInject".formatted(p.getName(), target.getName())
        );

      // This dependency can be late-inited
      if (aia.lateinit()) {
        // Already exists
        if (refs.containsKey(target)) {
          args[i] = refs.get(target);
          continue;
        }

        // Search for a matching member field
        Field f = null;
        for (Field tf : target.getDeclaredFields()) {
          if (!tf.getType().equals(p.getType()))
            continue;

          f = tf;
          break;
        }

        // Lateinits need to have a corresponding member field (same type)
        if (f == null)
          throw new RuntimeException(
            "Parameter %s of %s has no corresponding member field".formatted(p.getName(), target.getName())
          );

        // Add to buffer
        lateinitFields.add(f);
        continue;
      }

      // Directly inject main at this point
      if (dep.isAssignableFrom(plugin.getClass())) {
        args[i] = plugin;
        continue;
      }

      // Circular dependency (not resolved but already seen)
      if (seen.contains(dep))
        throw new RuntimeException(
          "Circular @AutoInject dependency encountered: %s of %s".formatted(dep.getName(), target.getName())
        );

      // Remember this dependency and resolve it's dependencies
      seen.add(dep);
      args[i] = createWithDependencies(plugin, ctorMap, dep, seen);
      seen.remove(dep);
    }

    // All constructor dependencies instantiated, now create the target itself
    // using all created dependencies
    Object inst = targetC.newInstance(args);

    // Add all fields as a tuple with their instance
    for (Field f : lateinitFields) {
      Class<?> t = f.getType();

      // Class not yet requested, create empty list
      if (!lateinits.containsKey(vanillaC))
        lateinits.put(t, new ArrayList<>());

      // Add the newly created object's lateinit request
      lateinits.get(t).add(new Tuple<>(inst, f));
    }

    onInstantiation(plugin, inst, vanillaC);
    refs.put(target, inst);

    return inst;
  }

  /**
   * Find the logger within the local list of refs
   * @return Logger instance or null if it's not yet constructed
   */
  private static ILogger findLogger() {
    // Look through ref-list
    for (Map.Entry<Class<?>, Object> ref : refs.entrySet()) {
      // This is a logger implementation
      if (ILogger.class.isAssignableFrom(ref.getKey()))
        return (ILogger) ref.getValue();
    }

    // Logger not yet available
    return null;
  }

  /**
   * Internal logging utility method, as no logger is existing at the absolute beginning.
   * Log messages get queued until the logger's instantiated
   * @param message Message to log
   */
  private static void logDebug(String message) {
    // Logger available
    ILogger logger = findLogger();
    if (logger != null) {
      // Empty out queue
      for (String msg : logQueue)
        logger.logDebug(msg);
      logQueue.clear();

      // Log latest message
      logger.logDebug(message);
      return;
    }

    // Queue message for later
    logQueue.add(message);
  }
}