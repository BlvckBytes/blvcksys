package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.events.IMoveListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Processes teleportation requests and either requires a no-move timeout
  period or fulfills the request instantly.
 */
@AutoConstruct
public class TeleportationHandler implements ITeleportationHandler, Listener {

  /**
   * Represents an active teleportation request of a player
   * @param taskHandle No-Move timeout task handle
   * @param cancelled Cancelled callback
   * @param moveListener Move listener registration ref
   */
  private record TeleportRequest(
    Integer taskHandle,
    Runnable cancelled,
    Runnable moveListener
  ) {}

  // Which animation to play during teleportation
  private static final AnimationType TELEPORT_ANIMATION = AnimationType.PURPLE_ROTATING_CONE;

  // Timeout in ticks for how long not to move during a teleportation
  private static final long TELEPORT_TIMEOUT = 20 * 3;

  private final Map<Player, TeleportRequest> tasks;

  private final ICombatLogHandler combatlog;
  private final IMoveListener move;
  private final IAnimationHandler animation;
  private final IObjectiveHandler obj;
  private final JavaPlugin plugin;
  private final IConfig cfg;

  public TeleportationHandler(
    @AutoInject IMoveListener move,
    @AutoInject IAnimationHandler animation,
    @AutoInject IObjectiveHandler obj,
    @AutoInject JavaPlugin plugin,
    @AutoInject IConfig cfg,
    @AutoInject ICombatLogHandler combatlog
  ) {
    this.move = move;
    this.animation = animation;
    this.obj = obj;
    this.plugin = plugin;
    this.cfg = cfg;
    this.combatlog = combatlog;

    this.tasks = new HashMap<>();
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  @Override
  public void requestTeleportation(Player p, Location to, @Nullable Runnable done, @Nullable Runnable cancelled) {

    // Instant teleportation allowed
    if (!combatlog.isInCombat(p) || PlayerPermission.TELEPORTATIONS_BYPASS.has(p)) {
      p.teleport(to);

      if (done != null)
        done.run();

      return;
    }

    // Cancel any previous teleportations
    cancelTeleportation(p, true);

    // Create a new teleportation timeout and teleport as well as remove after expiry
    int handle = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      p.teleport(to);

      if (done != null)
        done.run();

      cancelTeleportation(p, false);
    }, TELEPORT_TIMEOUT);

    // Start teleporting mode
    animation.startAnimation(p, null, TELEPORT_ANIMATION, null);
    obj.setBelowNameFlag(p, BelowNameFlag.TELEPORTING, true);

    p.sendMessage(
      cfg.get(ConfigKey.TELEPORTATIONS_INITIATED)
        .withPrefix()
        .asScalar()
    );

    // Listen for moves and cancel accordingly
    Runnable moveL = move.subscribe(p, () -> cancelTeleportation(p, true));
    tasks.put(p, new TeleportRequest(handle, cancelled, moveL));
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    cancelTeleportation(e.getPlayer(), true);
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Cancel a player's active teleportation, if it exists
   * @param p Target player
   * @param call Whether to call the cancelled callback and notify the player
   */
  private void cancelTeleportation(Player p, boolean call) {
    TeleportRequest req = tasks.remove(p);

    if (req == null)
      return;

    // Stop teleporting mode
    animation.stopAnimation(p, TELEPORT_ANIMATION);
    obj.setBelowNameFlag(p, BelowNameFlag.TELEPORTING, false);
    Bukkit.getScheduler().cancelTask(req.taskHandle);

    // Stop listening for any further moves
    move.unsubscribe(p, req.moveListener);

    if (call) {
      p.sendMessage(
        cfg.get(ConfigKey.TELEPORTATIONS_MOVED)
          .withPrefix()
          .asScalar()
        );

      if (req.cancelled != null)
        req.cancelled.run();
    }
  }
}
