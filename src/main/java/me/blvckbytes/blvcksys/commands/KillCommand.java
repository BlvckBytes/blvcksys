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

  Kill another player.
*/
@AutoConstruct
public class KillCommand extends APlayerCommand {

  public KillCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "kill",
      "Kill another player instantly",
      PlayerPermission.KILL,
      new CommandArgument("[player]", "Player to kill")
    );
  }

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOnlinePlayers(args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0);

    target.getWorld().strikeLightningEffect(target.getLocation());
    target.setHealth(0);

    // Inform the issuer
    p.sendMessage(
      cfg.get(ConfigKey.KILL_SENDER)
        .withPrefix()
        .withVariable("target", target.getName())
        .asScalar()
    );

    // Inform the player that just got killed
    target.sendMessage(
      cfg.get(ConfigKey.KILL_RECEIVER)
        .withPrefix()
        .withVariable("issuer", p.getName())
        .asScalar()
    );
  }
}
