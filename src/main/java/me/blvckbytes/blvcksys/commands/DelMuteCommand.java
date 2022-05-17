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
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Completely delete an existing mute.
*/
@AutoConstruct
public class DelMuteCommand extends APlayerCommand {

  private final IMuteHandler mutes;
  private final IPersistence pers;

  public DelMuteCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IMuteHandler mutes,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "delmute",
      "Delete a non-active mute completely",
      PlayerPermission.COMMAND_DELMUTE,
      new CommandArgument("<id>", "ID of the mute")
    );

    this.mutes = mutes;
    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all possible UUIDs
    if (currArg == 0)
      return suggestModels(args, currArg, MuteModel.class, "id", pers);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    UUID id = parseUUID(args, 0);
    Optional<MuteModel> mute = mutes.findById(id);

    if (mute.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.MUTE_UNKNOWN)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    if (mute.get().isActive()) {
      p.sendMessage(
        cfg.get(ConfigKey.MUTE_STILL_ACTIVE)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    pers.delete(mute.get());

    p.sendMessage(
      cfg.get(ConfigKey.MUTE_DELETED)
        .withPrefix()
        .withVariable("id", id)
        .asScalar()
    );
  }
}
