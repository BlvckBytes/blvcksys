package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.events.ITeleportListener;
import me.blvckbytes.blvcksys.handlers.ITeleportationHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 05/03/2022

  Go back to your previous teleport-location saved in your history.
 */
@AutoConstruct
public class BackCommand extends APlayerCommand {

  private final ITeleportListener tel;
  private final ITeleportationHandler tp;

  public BackCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ITeleportListener tel,
    @AutoInject ITeleportationHandler tp
  ) {
    super(
      plugin, logger, cfg, refl,
      "back",
      "Go back to your previous teleport-location",
      PlayerPermission.COMMAND_BACK
    );

    this.tel = tel;
    this.tp = tp;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Optional<Location> loc = tel.getHistoryPrevious(p);

    // No next location available
    if (loc.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.BACK_NONE)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    tp.requestTeleportation(p, loc.get(), () -> {
      p.sendMessage(
        cfg.get(ConfigKey.BACK_TELEPORTED)
          .withPrefix()
          .asScalar()
      );

    }, null);
  }
}
