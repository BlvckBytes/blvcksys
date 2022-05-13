package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/13/2022

  Revoke an existing, previously casted ban.
*/
@AutoConstruct
public class RevokeBanCommand extends APlayerCommand {

  public RevokeBanCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "revokeban",
      "Revoke a previously casted ban",
      PlayerPermission.COMMAND_BANREVOKE,
      new CommandArgument("<id>", "ID of the ban"),
      new CommandArgument("[reason]", "Reason of this punishment")
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String id = argval(args, 0);
    String reason = argvar(args, 1, "");
  }
}
