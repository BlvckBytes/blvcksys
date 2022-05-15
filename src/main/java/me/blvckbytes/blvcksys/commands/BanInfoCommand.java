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
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
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

  Display the specifics about an existing ban.
*/
@AutoConstruct
public class BanInfoCommand extends APlayerCommand {

  private final IPersistence pers;
  private final IBanHandler bans;

  public BanInfoCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers,
    @AutoInject IBanHandler bans
  ) {
    super(
      plugin, logger, cfg, refl,
      "baninfo",
      "Display the details of a ban",
      PlayerPermission.COMMAND_BANS,
      new CommandArgument("<id>", "UUID of the ban to display")
    );

    this.pers = pers;
    this.bans = bans;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return Stream.of(getArgumentPlaceholder(currArg));
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    UUID id = parseUUID(args, 0);

    Optional<BanModel> ban = pers.findFirst(
      new QueryBuilder<>(
        BanModel.class,
        "id", EqualityOperation.EQ, id
      )
    );

    if (ban.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.BAN_INFO_UNKNOWN)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    p.sendMessage(
      cfg.get(ConfigKey.BAN_INFO_SCREEN)
        .withPrefixes()
        .withVariables(bans.buildBanVariables(ban.get()))
        .asScalar()
    );
  }
}
