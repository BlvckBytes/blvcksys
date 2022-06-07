package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.gui.AnimationType;
import me.blvckbytes.blvcksys.handlers.gui.HomesGui;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  List all existing homes of yourself or another player.
*/
@AutoConstruct
public class HomesCommand extends APlayerCommand {

  private final HomesGui homesGui;

  public HomesCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject HomesGui homesGui
  ) {
    super(
      plugin, logger, cfg, refl,
      "homes",
      "Display all existing homes",
      null,
      new CommandArgument("[player]", "Name of the player to list homes from", PlayerPermission.COMMAND_HOMES_OTHERS)
    );

    this.homesGui = homesGui;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOfflinePlayers(args, currArg);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    OfflinePlayer target = offlinePlayer(args, 0, p);
    homesGui.show(p, target, AnimationType.SLIDE_UP);
  }
}
