package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AutoConstruct
public class JoinQuitListener implements Listener {

  private final IConfig cfg;

  public JoinQuitListener(
    @AutoInject IConfig cfg
  ) {
    this.cfg = cfg;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    e.setJoinMessage(
      cfg.get(ConfigKey.GENERIC_JOINED)
        .withPrefix()
        .withVariable("name", e.getPlayer().getDisplayName())
        .asScalar()
    );
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    e.setQuitMessage(
      cfg.get(ConfigKey.GENERIC_QUIT)
        .withPrefix()
        .withVariable("name", e.getPlayer().getDisplayName())
        .asScalar()
    );
  }
}
