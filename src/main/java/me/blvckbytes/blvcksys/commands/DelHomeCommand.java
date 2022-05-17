package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHomeHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.HomeModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldQueryGroup;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Delete an existing home by it's name.
*/
@AutoConstruct
public class DelHomeCommand extends APlayerCommand {

  private final IHomeHandler homes;
  private final IPersistence pers;

  public DelHomeCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IHomeHandler homes,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "delhome",
      "Delete an existing home",
      null,
      new CommandArgument("<name>", "Name of the home")
    );

    this.homes = homes;
    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestModels(args, currArg, HomeModel.class, "name", pers, new FieldQueryGroup(
        "creator__uuid", EqualityOperation.EQ, p.getUniqueId()
      ));

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);

    boolean succ = homes.deleteHome(p, name);

    p.sendMessage(
      cfg.get(succ ? ConfigKey.HOMES_DELETED : ConfigKey.HOMES_NOT_FOUND)
        .withPrefix()
        .withVariable("name", name)
        .asScalar()
    );
  }
}
