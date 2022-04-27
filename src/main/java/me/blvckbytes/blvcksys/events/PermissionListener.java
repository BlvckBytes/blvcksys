package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/25/2022

  Proxies modifying calls to the CraftPlayer's PermissibleBase's HashMap field
  called "permissions" in a non-modifying way, debounces those calls and then
  invokes a local handler routine. Permissions are diffed into separate added and
  removed lists for convenient access, which will be contained in the emitted
  PlayerPermissionsChangedEvent, which also offers all currently active permissions.
*/
@AutoConstruct
public class PermissionListener implements Listener, IAutoConstructed {

  // Ticks that need to elapse until the last modifying call is actually routed
  private static final long DEBOUNCE_TICKS = 10;

  private final MCReflect refl;
  private final JavaPlugin plugin;

  // Vanilla references of the proxied field for every player
  private final Map<Player, Object> vanillaRefs;

  // The previous permission list (last permission change call) for every player
  private final Map<Player, List<String>> previousPermissions;

  public PermissionListener(
    @AutoInject MCReflect refl,
    @AutoInject JavaPlugin plugin
  ) {
    this.refl = refl;
    this.plugin = plugin;

    this.vanillaRefs = new HashMap<>();
    this.previousPermissions = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void cleanup() {
    // Unproxy all players on unload
    for (Player t : Bukkit.getOnlinePlayers())
      unproxyPermissions(t);
  }

  @Override
  public void initialize() {
    // Proxy all players on load
    for (Player t : Bukkit.getOnlinePlayers())
      proxyPermissions(t);
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler(priority = EventPriority.LOWEST)
  public void onJoin(PlayerJoinEvent e) {
    // Proxy on join
    proxyPermissions(e.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onQuit(PlayerQuitEvent e) {
    // Unproxy on quit
    unproxyPermissions(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Fire the {@link PlayerPermissionsChangedEvent} after diffing the player's permission list
   * @param p Target player
   * @param permissions List of currently active permissions
   */
  private void fireEvent(Player p, List<String> permissions) {
    List<String> added = new ArrayList<>();
    List<String> removed = new ArrayList<>();
    List<String> previous = previousPermissions.getOrDefault(p, new ArrayList<>());

    for (String prev : previous) {
      // This permission was owned previously but is missing now
      if (!permissions.contains(prev))
        removed.add(prev);
    }

    for (String curr : permissions) {
      // This permission wasn't owned previously and thus has been added
      if (!previous.contains(curr))
        added.add(curr);
    }

    Bukkit.getPluginManager().callEvent(
      new PlayerPermissionsChangedEvent(p, permissions, added, removed)
    );
  }

  /**
   * Permission change handler, called whenever a player's permissions change
   * @param p Target player
   * @param permissions List of currently active permissions
   */
  private void onPermissionChange(Player p, List<String> permissions) {
    // Handle firing the delta event
    fireEvent(p, permissions);

    // Save these permissions as the previous state
    previousPermissions.put(p, permissions);
  }

  /**
   * Unproxy the permissions field for a given player by reverting
   * back to the vanilla reference
   * @param p Target player
   */
  private void unproxyPermissions(Player p) {
    // Get the vanilla reference from the local map, skip non-proxied players
    Object vanillaRef = vanillaRefs.remove(p);
    if (vanillaRef == null)
      return;

    // Restore the vanilla reference
    refl.getCraftPlayer(p)
      .flatMap(cp -> refl.getFieldByType(cp, PermissibleBase.class))
      .ifPresent(pb -> refl.setFieldByName(pb, "permissions", vanillaRef));
  }

  /**
   * Get a list of active permissions from a map of attachments
   * @param permissions Map of attachments
   * @return List of active permissions
   */
  private List<String> getPermissions(Map<String, PermissionAttachmentInfo> permissions) {
    return permissions.values()
      .stream()
      .filter(PermissionAttachmentInfo::getValue)
      .map(PermissionAttachmentInfo::getPermission)
      .toList();
  }

  /**
   * Create a new permission-field proxy for a given player
   * @param p Target player
   * @param permissions Vanilla map
   * @return New map that can replace the vanilla list value
   */
  private Object createPermissionProxy(Player p, Map<String, PermissionAttachmentInfo> permissions) {
    // Create a new proxied map
    return Proxy.newProxyInstance(
      permissions.getClass().getClassLoader(),
      new Class[]{ Map.class },

      // Create an anonymous implementation here, since it's pretty basic and too specific
      new InvocationHandler() {

        // Handle of the debounce task used to debounce map call bursts
        private int debounceTask = -1;

        // Lock to synchronize map calls (as I don't know all possible callers)
        private final ReentrantLock lock = new ReentrantLock();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          // Only act on modifying requests
          if (!(
            method.getName().equals("put") ||
            method.getName().equals("putAll") ||
            method.getName().equals("remove")
          ))
            return method.invoke(permissions, args);

          // Lock while operating on the map
          lock.lock();

          // Cancel the previous debounce task
          if (debounceTask >= 0) {
            Bukkit.getScheduler().cancelTask(debounceTask);
            debounceTask = -1;
          }

          // Create a new debounce task
          debounceTask = Bukkit.getScheduler().scheduleSyncDelayedTask(
            plugin, () -> onPermissionChange(p, getPermissions(permissions)), DEBOUNCE_TICKS
          );

          // Done with operations, unlock
          lock.unlock();

          return method.invoke(permissions, args);
        }
      }
    );
  }

  /**
   * Proxy the permissions field of a given player by setting a
   * read-only (non-modifying) proxy on the permissions map
   * @param p Target player
   */
  @SuppressWarnings("unchecked")
  private void proxyPermissions(Player p) {
    refl.getCraftPlayer(p)
      .flatMap(cp -> refl.getFieldByType(cp, PermissibleBase.class))
      .flatMap(pb ->
        refl.getFieldByName(pb, "permissions")
          .map(permissions -> new Tuple<>((PermissibleBase) pb, (Map<?, ?>) permissions))
      )
      .ifPresent(tuple -> {
        Map<String, PermissionAttachmentInfo> permissions = (Map<String, PermissionAttachmentInfo>) tuple.b();

        // Call initially
        onPermissionChange(p, getPermissions(permissions));

        // Set field to to the proxy reference
        if (refl.setFieldByName(tuple.a(), "permissions", createPermissionProxy(p, permissions)))
          // Save the vanilla reference
          this.vanillaRefs.put(p, permissions);
      });
  }
}
