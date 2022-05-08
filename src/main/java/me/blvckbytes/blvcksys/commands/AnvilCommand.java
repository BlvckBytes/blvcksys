package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.packets.communicators.container.ContainerType;
import me.blvckbytes.blvcksys.packets.communicators.container.IContainerCommunicator;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 05/01/2022

  Open a fully usable, virtual anvil.
 */
@AutoConstruct
public class AnvilCommand extends APlayerCommand {

  IContainerCommunicator anvil;

  public AnvilCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IContainerCommunicator anvil
  ) {
    super(
      plugin, logger, cfg, refl,
      "anvil",
      "Open a mobile anvil",
      PlayerPermission.COMMAND_ANVIL
    );

    this.anvil = anvil;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    this.anvil.openContainer(
      p, ContainerType.ANVIL,
      cfg.get(ConfigKey.ANVIL_GUINAME)
        .withVariable("owner", p.getName())
        .asScalar()
    );
  }
}
