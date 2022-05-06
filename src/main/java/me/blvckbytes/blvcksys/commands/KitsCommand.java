package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.KitModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Get a list of all available kits which shows additional
  personalized information on hovering the individual kits.
*/
@AutoConstruct
public class KitsCommand extends APlayerCommand {

  private IPersistence pers;

  public KitsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "kits",
      "List all existing kits",
      null
    );

    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    StringBuilder kitStr = new StringBuilder();

    List<KitModel> kits = pers.list(KitModel.class);

    for (int i = 0; i < kits.size(); i++)
      kitStr.append(kits.get(i).getName()).append(i == kits.size() - 1 ? "" : ", ");

    p.sendMessage("§aAvailable kits: " + kitStr);
  }
}
