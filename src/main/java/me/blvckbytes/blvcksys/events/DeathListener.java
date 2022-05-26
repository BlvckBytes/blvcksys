package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.commands.ISpawnCommand;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHologramHandler;
import me.blvckbytes.blvcksys.handlers.MultilineHologram;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Listens to death events and automatically respawns the player after
  the respawn delay elapsed. Does the same on joins if the player is dead.
  Also spawns kill indicators when a player has been killed by a hit and overrides
  the vanilla death messages.
*/
@AutoConstruct
public class DeathListener implements Listener {

  // Delay in ticks after death till the automatic respawn
  private final static long RESPAWN_DELAY_T = 15;

  // Time in ticks to display the upwards floating kill indicator for
  private static final long KILL_INDICATOR_DUR_T = 33;

  // Y velocity of the kill indicator
  private static final double KILL_INDICATOR_YVEL = 0.1;

  private final IConfig cfg;
  private final IHologramHandler holos;
  private final JavaPlugin plugin;
  private final ISpawnCommand spawn;
  private final IChatListener chat;

  public DeathListener(
    @AutoInject JavaPlugin plugin,
    @AutoInject ISpawnCommand spawn,
    @AutoInject IHologramHandler holos,
    @AutoInject IConfig cfg,
    @AutoInject IChatListener chat
  ) {
    this.plugin = plugin;
    this.spawn = spawn;
    this.holos = holos;
    this.cfg = cfg;
    this.chat = chat;
  }

  //=========================================================================//
  //                                  Listener                               //
  //=========================================================================//

  @EventHandler
  public void onDamage(EntityDamageByEntityEvent e) {
    // Didn't affect a player
    if (!(e.getEntity() instanceof Player p))
      return;

    // Didn't kill them
    if (p.getHealth() > e.getDamage())
      return;

    // Has been slain by another player
    if (e.getDamager() instanceof Player killer) {
      spawnKillIndicator(p, killer);

      chat.broadcastMessage(
        Bukkit.getOnlinePlayers(),
        cfg.get(ConfigKey.DEATH_MESSAGES_KILLED)
          .withPrefix()
          .withVariable("victim", p.getName())
          .withVariable("killer", killer.getName())
          .asScalar()
      );
      return;
    }
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent e) {
    Bukkit.getScheduler().runTaskLater(plugin, () ->
      spawn.getSpawn().ifPresent(spawn -> e.getPlayer().teleport(spawn))
    , 1);
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent e) {
    e.setDeathMessage(null);
    respawnPlayer(e.getEntity());
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    if (!e.getPlayer().isDead())
      return;

    respawnPlayer(e.getPlayer());
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Respawn the player after the delay elapsed
   * @param p Target player
   */
  private void respawnPlayer(Player p) {
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      p.spigot().respawn();
    }, RESPAWN_DELAY_T);
  }

  /**
   * Spawn a new kill indicating temporary hologram
   * @param victim Player that has been killed
   * @param killer Player that has killed
   */
  private void spawnKillIndicator(Player victim, Player killer) {
    MultilineHologram holo = holos.createTemporary(
      victim.getLocation(),
      List.of(killer),
      cfg.get(ConfigKey.KILL_INDICATORS)
        .withVariable("victim", victim.getName())
        .withVariable("coins", 10)
        .asList()
    );

    holo.setVelocity(new Vector(0, KILL_INDICATOR_YVEL, 0), 0D, false, false, null, null);
    Bukkit.getScheduler().runTaskLater(plugin, () -> holos.destroyTemporary(holo), KILL_INDICATOR_DUR_T);
  }

}
