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
  6reated On: 05/02/2022

  Open a fully usable, virtual workbench.
 */
@AutoConstruct
public class SmithingCommand extends APlayerCommand {

  private final IContainerCommunicator container;

  public SmithingCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IContainerCommunicator container
  ) {
    super(
      plugin, logger, cfg, refl,
      "smithing",
      "Open a mobile smithing table",
      PlayerPermission.COMMAND_SMITHING.toString()
    );

    this.container = container;
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    this.container.openContainer(
      p, ContainerType.SMITHING,
      cfg.get(ConfigKey.SMITHING_GUINAME)
        .withVariable("owner", p.getName())
        .asScalar()
    );
  }
}
