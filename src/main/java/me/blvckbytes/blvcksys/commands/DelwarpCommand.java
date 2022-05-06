package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.persistence.query.FieldQuery;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Delete an existing warp by it's name.
*/
@AutoConstruct
public class DelwarpCommand extends APlayerCommand {

  private final IPersistence pers;

  public DelwarpCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "delwarp",
      "Delete an existing warp",
      PlayerPermission.DELWARP,
      new CommandArgument("<name>", "Name of the warp")
    );

    this.pers = pers;
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);

    Optional<WarpModel> res = pers.findFirst(
      new QueryBuilder<>(
        WarpModel.class,
        "name", EqualityOperation.EQ_IC, name
      )
    );

    if (res.isEmpty()) {
      p.sendMessage("§cThere is no warp named '" + name + "'");
      return;
    }

    try {
      pers.delete(res.get());
      p.sendMessage("§aWarp '" + name + "' successfully deleted");
    } catch (Exception e) {
      logger.logError(e);
      internalError();
    }
  }
}
