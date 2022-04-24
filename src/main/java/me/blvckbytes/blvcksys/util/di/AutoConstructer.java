package me.blvckbytes.blvcksys.util.di;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
   * Execute the auto-constructor and thus instantiate all available
   * classes within the specified package that are annotated by @AutoConstruct
   *
   * @param pkg Package to search for targets in
   */
  public static void execute(JavaPlugin main, String pkg) {
    // Scan all classes in the target package and auto-close the scanner
    try (
      ScanResult result = new ClassGraph()
        .enableClassInfo()
        .enableAnnotationInfo()
        .acceptPackages(pkg)
        .scan()
    ) {
      // List of all target classes that require auto-construction
      List<? extends Class<?>> classes = result
        .getAllClasses()
        .filter(ci -> ci.hasAnnotation(AutoConstruct.class))
        .stream()
        .map(c -> {
          try {
            return Class.forName(c.getName());
          } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load @AutoConstruct class %s".formatted(c.getName()));
          }
        })
        .toList();

      // Mapping classes to a chosen constructor, which either has no deps or only @AutoInject dep parameters
      Map<Class<?>, Constructor<?>> ctorMap = selectConstructors(classes);

      // Resolve all dependencies recursively
      List<Class<?>> seen = new ArrayList<>();
      for (Map.Entry<Class<?>, Constructor<?>> e : ctorMap.entrySet())
        createWithDependencies(main, ctorMap, e.getKey(), seen);

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
   * @param main Reference of the JavaPlugin instance
   * @param instance Created object
   * @param vanillaC Vanilla class (unresolved interface for example) of this object
   */
  private static void onInstantiation(JavaPlugin main, Object instance, Class<?> vanillaC) {
    String name = instance.getClass().getName();
    logDebug("Created @AutoConstruct resource: " + name);

    // Check for lateinits that need this type
    List<Tuple<Object, Field>> receivers = lateinits.remove(vanillaC);
    if (receivers != null) {
      try {
        // Loop all tuples of object to field and set the value
        for (Tuple<Object, Field> fr : receivers) {
          Field f = fr.b();
          f.setAccessible(true);
          f.set(fr.a(), instance);

          logDebug("Injected lateinit dependency " + name + " into " + fr.a().getClass().getName());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // Also register events if the listener interface has been implemented
    if (instance instanceof Listener l) {
      main.getServer().getPluginManager().registerEvents(l, main);
      logDebug("Registered event-listener using handler: " + name);
    }
  }

  /**
   * Create a new instance of a class by creating all it's constructor's dependencies beforehand
   * @param main Reference of the JavaPlugin instance
   * @param ctorMap Constructor map of pre-selected, valid constructors
   * @param target Target class to construct
   * @param seen List of already seen classes, passed for recursion
   * @return Instantiated object ref
   *
   * @throws Exception Issues with instantiation or dependency conflicts
   */
  private static Object createWithDependencies(
    JavaPlugin main,
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
      onInstantiation(main, inst, vanillaC);
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
      if (dep.isAssignableFrom(main.getClass())) {
        args[i] = main;
        continue;
      }

      // Circular dependency (not resolved but already seen)
      if (seen.contains(dep))
        throw new RuntimeException(
          "Circular @AutoInject dependency encountered: %s of %s".formatted(dep.getName(), target.getName())
        );

      // Remember this dependency and resolve it's dependencies
      seen.add(dep);
      args[i] = createWithDependencies(main, ctorMap, dep, seen);
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

    onInstantiation(main, inst, vanillaC);
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