package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/13/2022

  Cast a new permanent ban upon a player.
*/
@AutoConstruct
public class BanCommand extends APlayerCommand {

  public BanCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "ban",
      "Cast a new ban upon a player",
      PlayerPermission.COMMAND_BAN,
      new CommandArgument("<name>", "Name of the target player"),
      new CommandArgument("[reason]", "Reason of this punishment")
    );
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
    String reason = argvar(args, 1, "");
  }
}
