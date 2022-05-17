package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHomeHandler;
import me.blvckbytes.blvcksys.persistence.models.HomeModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Set a new home at your current position with a specified unique name.
*/
@AutoConstruct
public class SetHomeCommand extends APlayerCommand {

  private final IHomeHandler homes;

  public SetHomeCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IHomeHandler homes
  ) {
    super(
      plugin, logger, cfg, refl,
      "sethome",
      "Create a new home at your current location",
      null,
      new CommandArgument("<name>", "Name of the home")
    );

    this.homes = homes;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);

    int numHomes = homes.countHomes(p);
    Integer maxHomes = PlayerPermission.COMMAND_SETHOME_MAX.getSuffixNumber(p, true).orElse(0);

    if (numHomes >= maxHomes && !PlayerPermission.COMMAND_SETHOME_MAX_BYPASS.has(p)) {
      p.sendMessage(
        cfg.get(ConfigKey.HOMES_MAX_REACHED)
          .withPrefix()
          .withVariable("num_max_homes", maxHomes)
          .asScalar()
      );
      return;
    }

    Optional<HomeModel> home = homes.createHome(p, name, p.getLocation());
    boolean succ = home.isPresent();

    p.sendMessage(
      cfg.get(succ ? ConfigKey.HOMES_CREATED : ConfigKey.HOMES_EXISTING)
        .withPrefix()
        .withVariable("name", name)
        .asScalar()
    );
  }
}
