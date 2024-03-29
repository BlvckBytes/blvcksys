package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IWarnHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.WarnModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Completely delete an existing warn.
*/
@AutoConstruct
public class DelWarnCommand extends APlayerCommand {

  private final IWarnHandler warns;
  private final IPersistence pers;

  public DelWarnCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IWarnHandler warns,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "delwarn",
      "Delete a non-active warn completely",
      PlayerPermission.COMMAND_DELWARN.toString(),
      new CommandArgument("<id>", "ID of the warn")
    );

    this.warns = warns;
    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all possible UUIDs
    if (currArg == 0)
      return suggestModels(args, currArg, WarnModel.class, "id", pers);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    UUID id = parseUUID(args, 0);
    Optional<WarnModel> warn = warns.findById(id);

    if (warn.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.WARN_UNKNOWN)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    if (warn.get().isActive()) {
      p.sendMessage(
        cfg.get(ConfigKey.WARN_STILL_ACTIVE)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    pers.delete(warn.get());

    p.sendMessage(
      cfg.get(ConfigKey.WARN_DELETED)
        .withPrefix()
        .withVariable("id", id)
        .asScalar()
    );
  }
}
