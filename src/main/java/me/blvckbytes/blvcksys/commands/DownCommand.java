package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
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
  Created On: 05/03/2022

  Teleport to the next block below your position.
 */
@AutoConstruct
public class DownCommand extends APlayerCommand {

  private final ITopCommand top;

  public DownCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ITopCommand top
  ) {
    super(
      plugin, logger, cfg, refl,
      "down",
      "Teleport to the next block below you",
      PlayerPermission.DOWN
    );

    this.top = top;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Optional<Location> lowerPoint = top.searchYSlice(p, true, true, true, false);

    p.sendMessage(
      cfg.get(lowerPoint.isPresent() ? ConfigKey.DOWN_TELEPORTED : ConfigKey.DOWN_VOID)
        .withPrefix()
        .asScalar()
    );

    lowerPoint
      .map(l -> l.add(0, 1, 0))
      .ifPresent(p::teleport);
  }
}
