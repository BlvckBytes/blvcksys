package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.events.IChatListener;
import me.blvckbytes.blvcksys.packets.communicators.hud.IHudCommunicator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Handles checking if player are in combat, where the combat mode only
  stops after a given timeout has elapsed. During combat, the player is informed
  of the remaining combat duration in their action-bar.
 */
@AutoConstruct
public class CombatLogHandler implements Listener, ICombatLogHandler, IAutoConstructed {

  // Timeout in milliseconds for how long a player is not allowed to fight
  // in order to have their combat timeout elapse
  private static final long COMBAT_TIMEOUT_MS = 1000 * 10;

  // Time in ticks between the remaining combatlog duration notification in the action-bar
  private static final long NOTIFIER_PERIOD = 10;

  // Mapping players to their last combat action timestamp
  private final Map<Player, Long> inCombat;
  private int notifierHandle;

  private final IConfig cfg;
  private final IChatListener chat;
  private final JavaPlugin plugin;
  private final IHudCommunicator hud;

  public CombatLogHandler(
    @AutoInject IConfig cfg,
    @AutoInject IChatListener chat,
    @AutoInject JavaPlugin plugin,
    @AutoInject IHudCommunicator hud
  ) {
    this.cfg = cfg;
    this.chat = chat;
    this.plugin = plugin;
    this.hud = hud;

    this.notifierHandle = -1;
    this.inCombat = new HashMap<>();
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  @Override
  public boolean isInCombat(Player p) {
    return inCombat.containsKey(p);
  }

  //=========================================================================//
  //                                  Listener                               //
  //=========================================================================//

  @EventHandler
  public void onDeath(PlayerDeathEvent e) {
    inCombat.remove(e.getEntity());
  }

  @EventHandler
  public void onDamage(EntityDamageByEntityEvent e) {
    if (!(e.getEntity() instanceof Player victim))
      return;

    if (!(e.getDamager() instanceof Player damager))
      return;

    inCombat.put(victim, System.currentTimeMillis());
    inCombat.put(damager, System.currentTimeMillis());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    Player p = e.getPlayer();

    if (!inCombat.containsKey(p))
      return;

    inCombat.remove(p);

    p.setHealth(0);

    // Strike a lightning effect at the player's position
    World w = p.getLocation().getWorld();
    if (w != null)
      w.strikeLightningEffect(p.getLocation());

    this.chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.COMBATLOG_BROADCAST)
        .withPrefix()
        .withVariable("name", p.getName())
        .asScalar()
    );
  }

  @Override
  public void cleanup() {
    if (notifierHandle > 0)
      Bukkit.getScheduler().cancelTask(notifierHandle);
  }

  @Override
  public void initialize() {
    notifierHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {

      for (Iterator<Player> playerI = inCombat.keySet().iterator(); playerI.hasNext();) {
        Player p = playerI.next();
        long remMS = inCombat.get(p) + COMBAT_TIMEOUT_MS - System.currentTimeMillis();

        if (remMS <= 0) {
          playerI.remove();

          hud.sendActionBar(
            p,
            cfg.get(ConfigKey.COMBATLOG_DONE)
              .withPrefix()
              .withVariable("remaining_seconds", 0)
              .asScalar()
          );

          continue;
        }

        hud.sendActionBar(
          p,
          cfg.get(ConfigKey.COMBATLOG_INFO)
            .withPrefix()
            .withVariable("remaining_seconds", remMS / 1000)
            .asScalar()
        );
      }

    }, 0L, NOTIFIER_PERIOD);
  }
}
