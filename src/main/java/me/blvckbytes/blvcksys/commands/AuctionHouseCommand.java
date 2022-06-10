package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.gui.AHGui;
import me.blvckbytes.blvcksys.handlers.gui.AnimationType;
import me.blvckbytes.blvcksys.packets.communicators.container.ContainerType;
import me.blvckbytes.blvcksys.packets.communicators.container.IContainerCommunicator;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 06/09/2022

  Opens the auction house management GUI.
 */
@AutoConstruct
public class AuctionHouseCommand extends APlayerCommand {

  private final AHGui ahGui;

  public AuctionHouseCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject AHGui ahGui
  ) {
    super(
      plugin, logger, cfg, refl,
      "auctionhouse,ah",
      "Open the auction house",
      null
    );

    this.ahGui = ahGui;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    this.ahGui.show(p, null, AnimationType.SLIDE_UP);
  }
}
