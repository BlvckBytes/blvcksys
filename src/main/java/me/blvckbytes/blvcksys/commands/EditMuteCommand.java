package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IMuteHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.MuteModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.TimeUtil;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Edit an existing mute's fields.
*/
@AutoConstruct
public class EditMuteCommand extends APlayerCommand {

  private enum MuteField {
    REASON,
    DURATION,
    REVOCATION_REASON
  }

  private final IPersistence pers;
  private final IMuteHandler mutes;
  private final TimeUtil time;

  public EditMuteCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers,
    @AutoInject IMuteHandler mutes,
    @AutoInject TimeUtil time
  ) {
    super(
      plugin, logger, cfg, refl,
      "editmute",
      "Edit a field of an existing mute",
      PlayerPermission.COMMAND_MUTE,
      new CommandArgument("<id>", "Name of the target player"),
      new CommandArgument("<field>", "Field to change"),
      new CommandArgument("<value>", "New value of the field")
    );

    this.pers = pers;
    this.mutes = mutes;
    this.time = time;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all possible UUIDs
    if (currArg == 0)
      return suggestModels(args, currArg, MuteModel.class, "id", pers);

    if (currArg == 1)
      return suggestEnum(args, currArg, MuteField.class);

    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    UUID id = parseUUID(args, 0);
    MuteModel mute = mutes.findById(id).orElse(null);

    if (mute == null) {
      p.sendMessage(
        cfg.get(ConfigKey.MUTE_UNKNOWN)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    MuteField field = parseEnum(MuteField.class, args, 1, null);
    String value = argvar(args, 2);

    switch (field) {
      case REASON -> mute.setReason(value);
      case DURATION -> mute.setDurationSeconds(time.parseDuration(value));
      case REVOCATION_REASON -> {
        if (!mute.isRevoked()) {
          p.sendMessage(
            cfg.get(ConfigKey.MUTE_NOT_REVOKED)
              .withPrefix()
              .withVariable("id", id)
              .asScalar()
          );
          return;
        }

        mute.setRevocationReason(value);
      }
    }

    pers.store(mute);
    p.sendMessage(
      cfg.get(ConfigKey.MUTE_EDIT_SAVED)
        .withPrefix()
        .withVariable("id", id)
        .asScalar()
    );
  }
}
