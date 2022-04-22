package me.blvckbytes.blvcksys.util.di;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import me.blvckbytes.blvcksys.Main;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;

public class AutoConstructer {

  // Cache for already constructed classes (singletons)
  private static final Map<Class<?>, Object> refs;

  static {
    refs = new HashMap<>();
  }

  /**
   * Execute the auto-constructor and thus instantiate all available
   * classes within the specified package that are annotated by @AutoConstruct
   *
   * @param pkg Package to search for targets in
   */
  public static void execute(String pkg) {
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
        createWithDependencies(ctorMap, e.getKey(), seen);

    } catch (Exception e) {
      Main.logger().logError(e);
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

      // Cannot construct this class, just skip it
      if (tarCtor == null) {
        Main.logger().logError("No valid @AutoConstruct constructors in %s", c.getName());
        continue;
      }

      // Register this constructor
      ctorMap.put(c, tarCtor);
    }

    return ctorMap;
  }

  /**
   * Create a new instance of a class by creating all it's constructor's dependencies beforehand
   * @param ctorMap Constructor map of pre-selected, valid constructors
   * @param target Target class to construct
   * @param seen List of already seen classes, passed for recursion
   * @return Instantiated object ref
   *
   * @throws Exception Issues with instantiation or dependency conflicts
   */
  private static Object createWithDependencies(
    Map<Class<?>, Constructor<?>> ctorMap,
    Class<?> target,
    List<Class<?>> seen
  ) throws Exception {
    // Resolve the interface implementation, if applicable
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
      refs.put(target, inst);

      // As this dependency now exists, remove it from the seen list, as it
      // cannot cause any further circular dependencies
      seen.remove(target);
      return inst;
    }

    // Loop all parameters of this constructor and thus all dependencies
    Object[] args = new Object[params.length];
    for (int i = 0; i < params.length; i++) {
      Class<?> dep = params[i].getType();

      // Circular dependency (not resolved but already seen)
      if (seen.contains(dep))
        throw new RuntimeException(
          "Circular @AutoInject dependency encountered: %s of %s".formatted(dep.getName(), target.getName())
        );

      // Remember this dependency and resolve it's dependencies
      seen.add(dep);
      args[i] = createWithDependencies(ctorMap, dep, seen);
    }

    // All constructor dependencies instantiated, now create the target itself
    // using all created dependencies
    Object inst = targetC.newInstance(args);
    refs.put(target, inst);

    return inst;
  }
}