package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.gui.AnimationType;
import me.blvckbytes.blvcksys.handlers.gui.FurnacesGui;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Manage your virtual furnaces by creating new instances or viewing existing ones.
*/
@AutoConstruct
public class FurnaceCommand extends APlayerCommand {

  private final FurnacesGui furnacesGui;

  public FurnaceCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject FurnacesGui furnacesGui
  ) {
    super(
      plugin, logger, cfg, refl,
      "furnace,fn",
      "Open your virtual furnace manager",
      PlayerPermission.COMMAND_FURNACE.toString()
    );

    this.furnacesGui = furnacesGui;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    furnacesGui.show(p, null, AnimationType.SLIDE_UP);
  }
}
