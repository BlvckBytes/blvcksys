package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.config.Config;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AutoConstruct
public class JoinQuitEvent implements Listener {

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    e.setJoinMessage(Config.getP(ConfigKey.GENERIC_JOINED, e.getPlayer().getDisplayName()));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    e.setQuitMessage(Config.getP(ConfigKey.GENERIC_QUIT, e.getPlayer().getDisplayName()));
  }
}
