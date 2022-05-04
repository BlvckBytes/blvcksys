package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.handlers.IObjectiveHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Show or hide the sidebar objective (broadly known as a scoreboard).
*/
@AutoConstruct
public class ScoreboardCommand extends APlayerCommand {

  private final IObjectiveHandler obj;

  public ScoreboardCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IObjectiveHandler obj
  ) {
    super(
      plugin, logger, cfg, refl,
      "scoreboard",
      "Show or hide your scoreboard",
      null
    );

    this.obj = obj;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    boolean state = !obj.getSidebarVisibility(p);
    obj.setSidebarVisibility(p, state);

    p.sendMessage(
      cfg.get(state ? ConfigKey.SCOREBOARD_SHOWN : ConfigKey.SCOREBOARD_HIDDEN)
        .withPrefix()
        .asScalar()
    );
  }
}
