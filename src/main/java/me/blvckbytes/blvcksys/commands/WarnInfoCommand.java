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
  Created On: 05/18/2022

  Display the specifics about an existing warn.
*/
@AutoConstruct
public class WarnInfoCommand extends APlayerCommand {

  private final IPersistence pers;
  private final IWarnHandler warns;

  public WarnInfoCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers,
    @AutoInject IWarnHandler warns
  ) {
    super(
      plugin, logger, cfg, refl,
      "warninfo",
      "Display the details of a warn",
      PlayerPermission.COMMAND_WARNS,
      new CommandArgument("<id>", "UUID of the warn to display")
    );

    this.pers = pers;
    this.warns = warns;
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

    p.sendMessage(
      cfg.get(ConfigKey.WARN_INFO_SCREEN)
        .withPrefixes()
        .withVariables(warns.buildWarnVariables(warn.get()))
        .asScalar()
    );
  }
}
