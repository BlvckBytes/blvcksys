package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHomeHandler;
import me.blvckbytes.blvcksys.handlers.ITeleportationHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.HomeModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldQueryGroup;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Teleport to an existing home by it's name.
*/
@AutoConstruct
public class HomeCommand extends APlayerCommand {

  private final IHomeHandler homes;
  private final IPersistence pers;
  private final ITeleportationHandler tp;

  public HomeCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IHomeHandler homes,
    @AutoInject IPersistence pers,
    @AutoInject ITeleportationHandler tp
  ) {
    super(
      plugin, logger, cfg, refl,
      "home",
      "Teleport to a home",
      null,
      new CommandArgument("<name>", "Name of the home"),
      new CommandArgument("[name]", "Owning player of this home")
    );

    this.homes = homes;
    this.pers = pers;
    this.tp = tp;
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

    if (currArg == 1)
      return suggestOfflinePlayers(args, currArg);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    OfflinePlayer owner = offlinePlayer(args, 1, p);

    Optional<HomeModel> home = homes.findHome(owner, name);

    if (home.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.HOMES_NOT_FOUND)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );

      return;
    }

    tp.requestTeleportation(p, home.get().getLoc(), () -> {
      p.sendMessage(
        cfg.get(owner.equals(p) ? ConfigKey.HOMES_TELEPORTED_SELF : ConfigKey.HOMES_TELEPORTED_OTHERS)
          .withPrefix()
          .withVariable("name", home.get().getName())
          .withVariable("owner", owner.getName())
          .asScalar()
      );
    }, null);
  }
}
