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
  Created On: 05/01/2022

  Teleport yourself or another player to a target player instantly.
*/
@AutoConstruct
public class TpCommand extends APlayerCommand {

  public TpCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "tp",
      "Teleport to a target instantly",
      PlayerPermission.TP,
      new CommandArgument("<target>", "Whom to teleport to"),
      new CommandArgument("[player]", "Player to teleport, defaults to yourself", PlayerPermission.TP_OTHERS)
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0 || currArg == 1)
      return suggestOnlinePlayers(p, args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0);
    Player player = onlinePlayer(args, 1, p);
    boolean isSelf = player.equals(p);

    // Teleport to the target
    player.teleport(target);

    // Inform the command sender
    p.sendMessage(
      cfg.get(isSelf ? ConfigKey.TP_SELF : ConfigKey.TP_OTHER_SENDER)
        .withPrefix()
        .withVariable("target", target.getName())
        .withVariable("player", player.getName())
        .asScalar()
    );

    // Inform the teleported player
    if (!isSelf)
      player.sendMessage(
        cfg.get(ConfigKey.TP_OTHER_RECEIVER)
          .withPrefix()
          .withVariable("target", target.getName())
          .withVariable("issuer", p.getName())
          .asScalar()
      );
  }
}
