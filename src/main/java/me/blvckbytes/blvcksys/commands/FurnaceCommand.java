package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IVirtualFurnaceHandler;
import me.blvckbytes.blvcksys.handlers.gui.VirtualFurnaceGui;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Manage virtual furnaces.
*/
@AutoConstruct
public class FurnaceCommand extends APlayerCommand {

  private final VirtualFurnaceGui virtualFurnaceGui;
  private final IVirtualFurnaceHandler virtualFurnaceHandler;

  public FurnaceCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject VirtualFurnaceGui virtualFurnaceGui,
    @AutoInject IVirtualFurnaceHandler virtualFurnaceHandler
  ) {
    super(
      plugin, logger, cfg, refl,
      "furnace,fn",
      "Open a virtual furnace",
      PlayerPermission.COMMAND_FURNACE,
      new CommandArgument("<index>", "Index number of the virtual furnace")
    );

    this.virtualFurnaceGui = virtualFurnaceGui;
    this.virtualFurnaceHandler = virtualFurnaceHandler;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    int index = parseInt(args, 0);
    virtualFurnaceGui.show(p, virtualFurnaceHandler.accessFurnace(p, index), null);
  }
}
