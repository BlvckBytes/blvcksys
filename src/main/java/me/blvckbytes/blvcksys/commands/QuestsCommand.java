package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.gui.AnimationType;
import me.blvckbytes.blvcksys.handlers.gui.QuestsGui;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/26/2022

  Opens the quests GUI for a player.
*/
@AutoConstruct
public class QuestsCommand extends APlayerCommand {

  private final QuestsGui questsGui;

  public QuestsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject QuestsGui questsGui
  ) {
    super(
      plugin, logger, cfg, refl,
      "quests",
      "View all existing quests",
      null
    );

    this.questsGui = questsGui;
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    questsGui.show(p, null, AnimationType.SLIDE_UP);
  }
}
