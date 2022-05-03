package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.events.ITeleportListener;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 05/03/2022

  Go forward to your next teleport-location saved in your history.
 */
@AutoConstruct
public class ForwardCommand extends APlayerCommand {

  private final ITeleportListener tel;

  public ForwardCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ITeleportListener tel
  ) {
    super(
      plugin, logger, cfg, refl,
      "forward",
      "Go forward to your next teleport-location",
      PlayerPermission.FORWARD
    );

    this.tel = tel;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Optional<Location> loc = tel.getHistoryNext(p);
    boolean exists = loc.isPresent();

    p.sendMessage(
      cfg.get(exists ? ConfigKey.FORWARD_TELEPORTED : ConfigKey.FORWARD_NONE)
        .withPrefix()
        .asScalar()
    );

    // No next location available
    if (!exists)
      return;

    p.teleport(loc.get());
  }
}
