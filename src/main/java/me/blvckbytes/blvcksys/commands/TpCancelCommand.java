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

  Cancel the pending teleport request you've already sent out.
*/
@AutoConstruct
public class TpCancelCommand extends APlayerCommand {

  private final ITpaCommand tpa;

  public TpCancelCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ITpaCommand tpa
  ) {
    super(
      plugin, logger, cfg, refl,
      "tpcancel",
      "Cancel a request you've already sent",
      null,
      new CommandArgument("<player>", "Request target to cancel")
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

    // Cancel this request
    if (!tpa.cancelRequest(p, target)) {

      // Hasn't setn a request to this player yet
      p.sendMessage(
        cfg.get(ConfigKey.TPA_NONE_SENT)
          .withPrefix()
          .withVariable("target", target.getName())
          .asScalar()
      );
    }
  }
}
