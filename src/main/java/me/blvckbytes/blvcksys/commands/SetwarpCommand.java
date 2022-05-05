package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Set a new warp at your current position with a specified unique name.
*/
@AutoConstruct
public class SetwarpCommand extends APlayerCommand {

  private final IPersistence pers;

  public SetwarpCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "setwarp",
      "Create a new warp at your current location",
      PlayerPermission.SETWARP,
      new CommandArgument("<name>", "Name of the warp")
    );

    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    Location l = p.getLocation();

    try {
      WarpModel warp = new WarpModel(name, l, p);
      pers.store(warp);

      p.sendMessage(
        cfg.get(ConfigKey.WARP_CREATED)
          .withPrefix()
          .withVariable("name", name)
          .withVariable("location", "(" + l.getBlockX() + " | " + l.getBlockY() + " | " + l.getBlockZ() + ")")
          .asScalar()
      );
    }
    catch (PersistenceException e) {
      // A warp with this name already exists
      if (e instanceof DuplicatePropertyException d && d.getProperty().equals("name")) {
        p.sendMessage(
          cfg.get(ConfigKey.WARP_EXISTS)
            .withPrefix()
            .withVariable("name", d.getValue())
            .asScalar()
        );
        return;
      }

      internalError();
    }
  }
}
