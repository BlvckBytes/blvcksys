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

  Teleport to the lowest block at your position.
 */
@AutoConstruct
public class BottomCommand extends APlayerCommand {

  private final ITopCommand top;

  public BottomCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ITopCommand top
  ) {
    super(
      plugin, logger, cfg, refl,
      "bottom",
      "Teleport to the lowest block below you",
      PlayerPermission.COMMAND_BOTTOM.toString()
    );

    this.top = top;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Optional<Location> lowestPoint = top.searchYSlice(p, true, true, true, true);

    p.sendMessage(
      cfg.get(lowestPoint.isPresent() ? ConfigKey.BOTTOM_TELEPORTED : ConfigKey.BOTTOM_VOID)
        .withPrefix()
        .asScalar()
    );

    lowestPoint
      .map(l -> l.add(0, 1, 0))
      .ifPresent(p::teleport);
  }
}
