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
import me.blvckbytes.blvcksys.util.TimeUtil;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  Edit an existing warn's fields.
*/
@AutoConstruct
public class EditWarnCommand extends APlayerCommand {

  private enum WarnField {
    REASON,
    DURATION,
    REVOCATION_REASON
  }

  private final IPersistence pers;
  private final IWarnHandler warns;
  private final TimeUtil time;

  public EditWarnCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers,
    @AutoInject IWarnHandler warns,
    @AutoInject TimeUtil time
  ) {
    super(
      plugin, logger, cfg, refl,
      "editwarn",
      "Edit a field of an existing warn",
      PlayerPermission.COMMAND_WARN.toString(),
      new CommandArgument("<id>", "Name of the target player"),
      new CommandArgument("<field>", "Field to change"),
      new CommandArgument("<value>", "New value of the field")
    );

    this.pers = pers;
    this.warns = warns;
    this.time = time;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all possible UUIDs
    if (currArg == 0)
      return suggestModels(args, currArg, WarnModel.class, "id", pers);

    if (currArg == 1)
      return suggestEnum(args, currArg, WarnField.class);

    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    UUID id = parseUUID(args, 0);
    WarnModel warn = warns.findById(id).orElse(null);

    if (warn == null) {
      p.sendMessage(
        cfg.get(ConfigKey.WARN_UNKNOWN)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    WarnField field = parseEnum(WarnField.class, args, 1, null);
    String value = argvar(args, 2);

    switch (field) {
      case REASON -> warn.setReason(value);
      case DURATION -> warn.setDurationSeconds(time.parseDuration(value));
      case REVOCATION_REASON -> {
        if (!warn.isRevoked()) {
          p.sendMessage(
            cfg.get(ConfigKey.WARN_NOT_REVOKED)
              .withPrefix()
              .withVariable("id", id)
              .asScalar()
          );
          return;
        }

        warn.setRevocationReason(value);
      }
    }

    pers.store(warn);
    p.sendMessage(
      cfg.get(ConfigKey.WARN_EDIT_SAVED)
        .withPrefix()
        .withVariable("id", id)
        .asScalar()
    );
  }
}
