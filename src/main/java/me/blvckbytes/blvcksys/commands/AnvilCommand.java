package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.communicators.anvil.IAnvilCommunicator;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
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

  IAnvilCommunicator anvil;

  public AnvilCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IAnvilCommunicator anvil
  ) {
    super(
      plugin, logger, cfg, refl,
      "anvil",
      "Open a mobile anvil",
      PlayerPermission.ANVIL
    );

    this.anvil = anvil;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    this.anvil.openFunctionalAnvil(
      p,
      cfg.get(ConfigKey.ANVIL_GUINAME)
        .withVariable("owner", p.getName())
        .asScalar()
    );
  }
}
