package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.commands.ISpawnCommand;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Listens to death events and automatically respawns the player after
  the respawn delay elapsed. Does the same on joins if the player is dead.
*/
@AutoConstruct
public class DeathListener implements Listener {

  // Delay in ticks after death till the automatic respawn
  private final static long RESPAWN_DELAY_T = 15;

  private final JavaPlugin plugin;
  private final ISpawnCommand spawn;

  public DeathListener(
    @AutoInject JavaPlugin plugin,
    @AutoInject ISpawnCommand spawn
  ) {
    this.plugin = plugin;
    this.spawn = spawn;
  }

  //=========================================================================//
  //                                  Listener                               //
  //=========================================================================//

  @EventHandler
  public void onRespawn(PlayerRespawnEvent e) {
    Bukkit.getScheduler().runTaskLater(plugin, () ->
      spawn.getSpawn().ifPresent(spawn -> e.getPlayer().teleport(spawn))
    , 1);
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent e) {
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
}
