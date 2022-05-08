package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
      PlayerPermission.COMMAND_DELWARP,
      new CommandArgument("<name>", "Name of the warp")
    );

    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest available warps
    if (currArg == 0) {
      return pers.listRaw(WarpModel.class, "name")
        .stream()
        .map(warp -> warp.get("name"))
        .filter(Objects::nonNull)
        .map(Objects::toString);
    }

    return super.onTabCompletion(p, args, currArg);
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
      p.sendMessage(
        cfg.get(ConfigKey.WARP_NOT_EXISTING)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    WarpModel warp = res.get();

    pers.delete(warp);
    p.sendMessage(
      cfg.get(ConfigKey.WARP_DELETED)
        .withPrefix()
        .withVariable("name", warp.getName())
        .asScalar()
    );
  }
}
