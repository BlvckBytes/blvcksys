package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IWarnHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Cast a new permanent warn upon a player.
*/
@AutoConstruct
public class WarnCommand extends APlayerCommand {

  private final IWarnHandler warns;

  public WarnCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IWarnHandler warns
  ) {
    super(
      plugin, logger, cfg, refl,
      "warn",
      "Cast a new warn upon a player",
      PlayerPermission.COMMAND_WARN,
      new CommandArgument("<name>", "Name of the target player"),
      new CommandArgument("[reason]", "Reason of this punishment")
    );

    this.warns = warns;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOfflinePlayers(args, currArg);

    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    OfflinePlayer target = offlinePlayer(args, 0);

    int hasCount = warns.countActiveWarns(target);
    int maxWarns = warns.getMaxActiveWarns();
    if (hasCount >= maxWarns) {
      p.sendMessage(
        cfg.get(ConfigKey.WARN_MAX_REACHED)
          .withVariable("target", target.getName())
          .withVariable("max_warns", maxWarns)
          .asScalar()
      );
      return;
    }

    String reason = argvar(args, 1, "");
    warns.broadcastWarn(warns.createWarn(p, target, null, reason));
  }
}
