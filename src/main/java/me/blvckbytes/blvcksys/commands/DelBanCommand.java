package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IBanHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.BanModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/16/2022

  Completely delete an existing ban.
*/
@AutoConstruct
public class DelBanCommand extends APlayerCommand {

  private final IBanHandler bans;
  private final IPersistence pers;

  public DelBanCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IBanHandler bans,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "delban",
      "Delete a non-active ban completely",
      PlayerPermission.COMMAND_DELBAN,
      new CommandArgument("<id>", "ID of the ban")
    );

    this.bans = bans;
    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all possible UUIDs
    if (currArg == 0)
      return suggestModels(args, currArg, BanModel.class, "id", pers);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    UUID id = parseUUID(args, 0);
    Optional<BanModel> ban = bans.findById(id);

    if (ban.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.BAN_UNKNOWN)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    if (ban.get().isActive()) {
      p.sendMessage(
        cfg.get(ConfigKey.BAN_STILL_ACTIVE)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    pers.delete(ban.get());

    p.sendMessage(
      cfg.get(ConfigKey.BAN_DELETED)
        .withPrefix()
        .withVariable("id", id)
        .asScalar()
    );
  }
}
