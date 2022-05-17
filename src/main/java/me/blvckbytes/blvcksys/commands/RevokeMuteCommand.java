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

  Revoke an existing, previously casted mute.
*/
@AutoConstruct
public class RevokeMuteCommand extends APlayerCommand {

  private final IMuteHandler mutes;
  private final IPersistence pers;

  public RevokeMuteCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IMuteHandler mutes,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "revokemute",
      "Revoke a previously casted mute",
      PlayerPermission.COMMAND_MUTEREVOKE,
      new CommandArgument("<id>", "ID of the mute"),
      new CommandArgument("[reason]", "Reason of this revocation")
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

    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    UUID id = parseUUID(args, 0);
    String reason = argvar(args, 1, "");
    Optional<MuteModel> mute = mutes.findById(id);

    if (reason.isBlank())
      reason = null;

    if (mute.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.MUTE_UNKNOWN)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    MuteModel revoked = mutes.revokeMute(mute.get(), p, reason);

    if (revoked == null) {
      p.sendMessage(
        cfg.get(ConfigKey.MUTE_ALREADY_REVOKED)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    mutes.broadcastRevoke(revoked);
  }
}
