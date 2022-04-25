package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.packets.modifiers.tablist.ITabGroupManager;
import me.blvckbytes.blvcksys.packets.modifiers.tablist.TabListGroup;
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
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@AutoConstruct
public class PermissionListener implements Listener, IAutoConstructed {

  private final MCReflect refl;
  private final JavaPlugin plugin;
  private final Map<Player, Object> vanillaRefs;
  private final ITabGroupManager gm;

  public PermissionListener(
    @AutoInject MCReflect refl,
    @AutoInject JavaPlugin plugin,
    @AutoInject ITabGroupManager gm
  ) {
    this.refl = refl;
    this.plugin = plugin;
    this.gm = gm;
    this.vanillaRefs = new HashMap<>();
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

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Permission change handler, called whenever a player's permissions change
   * @param p Target player
   * @param permissions List of currently active permissions
   */
  private void onPermissionChange(Player p, List<String> permissions) {
    List<String> currGroups = permissions.stream()
      .filter(perm -> perm.startsWith("group."))
      .map(perm -> perm.substring(perm.indexOf('.') + 1))
      .toList();

    // Decide the group that will be displayed (lowest priority -> most important)
    TabListGroup displayGroup = null;
    for (String permission : permissions) {
      // Only search for group meta-permissions
      if (!permission.startsWith("group."))
        continue;

      // Get the group by it's name
      String groupName = permission.substring(permission.indexOf('.') + 1);
      Optional<TabListGroup> tg = gm.getGroup(groupName);

      // Unknown group
      if (tg.isEmpty())
        continue;

      // Update the dispalygroup initially and for every group of higher importance
      if (displayGroup == null || displayGroup.priority() > tg.get().priority())
        displayGroup = tg.get();
    }

    // In no known group, reset back to default
    if (displayGroup == null)
      gm.resetPlayerGroup(p);

    // Set the player's group
    else
      gm.setPlayerGroup(displayGroup, p);
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
      .ifPresent(tuple -> proxyPermissions(p, tuple.a(), (Map<String, PermissionAttachmentInfo>) tuple.b()));
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
            plugin, () -> onPermissionChange(p, getPermissions(permissions)), 10
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
   * @param pb PermissibleBase of this player
   * @param permissions Vanilla permission map
   */
  private void proxyPermissions(Player p, PermissibleBase pb, Map<String, PermissionAttachmentInfo> permissions) {
    // Call initially
    onPermissionChange(p, getPermissions(permissions));

    // Set field to to the proxy reference
    if (refl.setFieldByName(pb, "permissions", createPermissionProxy(p, permissions)))
      // Save the vanilla reference
      this.vanillaRefs.put(p, permissions);
  }
}
