package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Change the time of your own client to either day, night or the server's.
*/
@AutoConstruct
public class PTimeCommand extends APlayerCommand implements IPTimeCommand {

  public PTimeCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "ptime",
      "Change the time of your client",
      PlayerPermission.COMMAND_PTIME,
      new CommandArgument("<time>", "The time to set"),
      new CommandArgument("[player]", "Target player", PlayerPermission.COMMAND_PTIME_OTHERS)
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all shorthands
    if (currArg == 0)
      return suggestEnum(args, currArg, TimeShorthand.class);

    // Suggest all online players
    if (currArg == 1)
      return suggestOnlinePlayers(p, args, currArg, false);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    TimeShorthand time = parseEnum(TimeShorthand.class, args, 0, null);
    Player target = onlinePlayer(args, 1, p);

    setTime(p, target, time);

    // Not the dispatcher, inform about change for others
    if (!target.equals(p))
      p.sendMessage(
        cfg.get(time == TimeShorthand.RESET ? ConfigKey.PTIME_RESET_OTHERS_SENDER : ConfigKey.PTIME_SET_OTHERS_SENDER)
          .withPrefix()
          .withVariable("target", target.getName())
          .withVariable("time", time.name())
          .asScalar()
      );
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void setTime(Player dispatcher, Player client, TimeShorthand shorthand) {
    if (shorthand == TimeShorthand.RESET)
      client.resetPlayerTime();
    else
      client.setPlayerTime(shorthand.getTime(), false);

    // Inform the client about the change
    client.sendMessage(
      cfg.get(
          // Time has been changed for the dispatcher themselves
          dispatcher.equals(client) ? (shorthand == TimeShorthand.RESET ? ConfigKey.PTIME_RESET : ConfigKey.PTIME_SET) :
          // Time has been changed for someone else
          (shorthand == TimeShorthand.RESET ? ConfigKey.PTIME_RESET_OTHERS_RECEIVER : ConfigKey.PTIME_SET_OTHERS_RECEIVER)
        )
        .withPrefix()
        .withVariable("target", client.getName())
        .withVariable("issuer", dispatcher.getName())
        .withVariable("time", shorthand.name())
        .asScalar()
    );
  }
}
