package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
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
  Created On: 05/03/2022

  Teleport to the next block above your position.
 */
@AutoConstruct
public class UpCommand extends APlayerCommand {

  private final ITopCommand top;

  public UpCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ITopCommand top
  ) {
    super(
      plugin, logger, cfg, refl,
      "up",
      "Teleport to the next block above you",
      PlayerPermission.UP
    );

    this.top = top;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Optional<Location> higherPoint = top.searchYSlice(p, false, false, true, false);

    p.sendMessage(
      cfg.get(higherPoint.isPresent() ? ConfigKey.UP_TELEPORTED : ConfigKey.UP_AIR)
        .withPrefix()
        .asScalar()
    );

    higherPoint
      .map(l -> l.add(0, 1, 0))
      .ifPresent(p::teleport);
  }
}
