package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Deny a pending incoming teleport request.
*/
@AutoConstruct
public class TpDenyCommand extends APlayerCommand {

  private final ITpaCommand tpa;

  public TpDenyCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ITpaCommand tpa
  ) {
    super(
      plugin, logger, cfg, refl,
      "tpdeny",
      "Deny an incoming tpa request",
      null,
      new CommandArgument("<player>", "Request origin to deny")
    );

    this.tpa = tpa;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOnlinePlayers(p, args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0);

    // Deny this request
    if (!tpa.denyRequest(target, p)) {

      // Hasn't received a request from this player yet
      p.sendMessage(
        cfg.get(ConfigKey.TPA_NONE_RECEIVED)
        .withPrefix()
        .withVariable("sender", target.getName())
        .asScalar()
      );
    }
  }
}
