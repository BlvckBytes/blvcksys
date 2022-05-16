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
import me.blvckbytes.blvcksys.util.TimeUtil;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/13/2022

  Edit an existing ban's fields.
*/
@AutoConstruct
public class EditBanCommand extends APlayerCommand {

  private enum BanField {
    REASON,
    DURATION,
    REVOCATION_REASON
  }

  private final IPersistence pers;
  private final IBanHandler bans;
  private final TimeUtil time;

  public EditBanCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers,
    @AutoInject IBanHandler bans,
    @AutoInject TimeUtil time
  ) {
    super(
      plugin, logger, cfg, refl,
      "editban",
      "Edit a field of an existing ban",
      PlayerPermission.COMMAND_BAN,
      new CommandArgument("<id>", "Name of the target player"),
      new CommandArgument("<field>", "Field to change"),
      new CommandArgument("<value>", "New value of the field")
    );

    this.pers = pers;
    this.bans = bans;
    this.time = time;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all possible UUIDs
    if (currArg == 0)
      return suggestModels(args, currArg, BanModel.class, "id", pers);

    if (currArg == 1)
      return suggestEnum(args, currArg, BanField.class);

    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    UUID id = parseUUID(args, 0);
    BanModel ban = bans.findById(id).orElse(null);

    if (ban == null) {
      p.sendMessage(
        cfg.get(ConfigKey.BAN_UNKNOWN)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    BanField field = parseEnum(BanField.class, args, 1, null);
    String value = argvar(args, 2);

    switch (field) {
      case REASON -> ban.setReason(value);
      case DURATION -> {
        if (ban.getDurationSeconds() == null) {
          p.sendMessage(
            cfg.get(ConfigKey.BAN_IS_PERMANENT)
              .withPrefix()
              .withVariable("id", id)
              .asScalar()
          );
          return;
        }

        ban.setDurationSeconds(time.parseDuration(value));
      }
      case REVOCATION_REASON -> {
        if (ban.getRevoker() == null) {
          p.sendMessage(
            cfg.get(ConfigKey.BAN_NOT_REVOKED)
              .withPrefix()
              .withVariable("id", id)
              .asScalar()
          );
          return;
        }

        ban.setRevocationReason(value);
      }
    }

    pers.store(ban);
    p.sendMessage(
      cfg.get(ConfigKey.BAN_EDIT_SAVED)
        .withPrefix()
        .withVariable("id", id)
        .asScalar()
    );
  }
}
