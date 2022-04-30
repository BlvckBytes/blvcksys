package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Change the time of the world you're currently in to either day or night
*/
@AutoConstruct
public class DayNightCommand extends APlayerCommand {

  private final ITimeCommand time;

  public DayNightCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ITimeCommand time
  ) {
    super(
      plugin, logger, cfg, refl,
      "day,night",
      "Change the time of the world you're in",
      PlayerPermission.COMMAND_TIME
    );

    this.time = time;
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    if (label.equals("day"))
      time.setTime(p, p.getWorld(), TimeShorthand.DAY);

    else if (label.equals("night"))
      time.setTime(p, p.getWorld(), TimeShorthand.NIGHT);
  }
}
