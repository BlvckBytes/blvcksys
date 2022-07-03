package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Change the time of the world you're currently in
*/
@AutoConstruct
public class TimeCommand extends APlayerCommand implements ITimeCommand {

  public TimeCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "time",
      "Change the time of the world you're in",
      PlayerPermission.COMMAND_TIME.toString(),
      new CommandArgument("<time>", "Time to set")
    );
  }

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all shorthands
    if (currArg == 0)
      return suggestEnum(args, currArg, TimeShorthand.class);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    TimeShorthand time = parseEnum(TimeShorthand.class, args, 0, TimeShorthand.DAY);

    // Set the time
    setTime(p, p.getWorld(), time);

  }

  @Override
  public void setTime(@Nullable Player dispatcher, World world, TimeShorthand shorthand) {
    world.setTime(shorthand.getTime());

    if (dispatcher == null)
      return;

    // Notify all affected players
    for (Player affected : world.getPlayers())
      affected.sendMessage(
        cfg.get(ConfigKey.TIME_SET)
          .withPrefix()
          .withVariable("issuer", dispatcher.getName())
          .withVariable("time", shorthand.toString())
          .asScalar()
      );
  }
}
