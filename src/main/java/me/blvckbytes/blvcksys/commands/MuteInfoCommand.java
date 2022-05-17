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

  Display the specifics about an existing mute.
*/
@AutoConstruct
public class MuteInfoCommand extends APlayerCommand {

  private final IPersistence pers;
  private final IMuteHandler mutes;

  public MuteInfoCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers,
    @AutoInject IMuteHandler mutes
  ) {
    super(
      plugin, logger, cfg, refl,
      "muteinfo",
      "Display the details of a mute",
      PlayerPermission.COMMAND_MUTE,
      new CommandArgument("<id>", "UUID of the mute to display")
    );

    this.pers = pers;
    this.mutes = mutes;
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

    p.sendMessage(
      cfg.get(ConfigKey.MUTE_INFO_SCREEN)
        .withPrefixes()
        .withVariables(mutes.buildMuteVariables(mute.get()))
        .asScalar()
    );
  }
}
