package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.ICombatLogHandler;
import me.blvckbytes.blvcksys.handlers.gui.AnimationType;
import me.blvckbytes.blvcksys.handlers.gui.EnderchestGui;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  Allows players to access their enderchest by a command.
*/
@AutoConstruct
public class EnderchestCommand extends APlayerCommand implements IEnderchestCommand {

  private final ICombatLogHandler combatLog;
  private final EnderchestGui enderchestGui;

  public EnderchestCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ICombatLogHandler combatLog,
    @AutoInject EnderchestGui enderchestGui
  ) {
    super(
      plugin, logger, cfg, refl,
      "enderchest,ec",
      "Access your enderchest",
      null,
      new CommandArgument("<player>", "Player to access", PlayerPermission.COMMAND_ENDERCHEST_OTHERS.toString())
    );

    this.combatLog = combatLog;
    this.enderchestGui = enderchestGui;
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
    openEnderchest(p, target);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void openEnderchest(Player executor) {
    openEnderchest(executor, executor);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Open a target player's enderchest to an executing player
   * @param executor Player to open for
   * @param target Enderchest owner
   */
  private void openEnderchest(Player executor, OfflinePlayer target) {
    if (combatLog.isInCombat(executor)) {
      executor.sendMessage(
        cfg.get(ConfigKey.ENDERCHEST_IN_COMBAT)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    enderchestGui.show(executor, target, AnimationType.SLIDE_DOWN);
  }
}
