package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.commands.IVanishCommand;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Overrides the vanilla join- and quit messages by config values.
*/
@AutoConstruct
public class JoinQuitListener implements Listener {

  private final IConfig cfg;
  private final IVanishCommand vanish;

  public JoinQuitListener(
    @AutoInject IConfig cfg,
    @AutoInject IVanishCommand vanish
  ) {
    this.cfg = cfg;
    this.vanish = vanish;
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

  @EventHandler(priority = EventPriority.LOWEST)
  public void onQuit(PlayerQuitEvent e) {
    // Don't print quit messages for vanished players
    if (vanish.isVanished(e.getPlayer())) {
      e.setQuitMessage(null);
      return;
    }

    e.setQuitMessage(
      cfg.get(ConfigKey.GENERIC_QUIT)
        .withPrefix()
        .withVariable("name", e.getPlayer().getDisplayName())
        .asScalar()
    );
  }
}
