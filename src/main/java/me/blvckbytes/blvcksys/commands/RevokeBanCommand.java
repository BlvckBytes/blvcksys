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
  Created On: 05/13/2022

  Revoke an existing, previously casted ban.
*/
@AutoConstruct
public class RevokeBanCommand extends APlayerCommand {

  private final IBanHandler bans;
  private final IPersistence pers;

  public RevokeBanCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IBanHandler bans,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "revokeban",
      "Revoke a previously casted ban",
      PlayerPermission.COMMAND_BANREVOKE,
      new CommandArgument("<id>", "ID of the ban"),
      new CommandArgument("[reason]", "Reason of this revocation")
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

    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    UUID id = parseUUID(args, 0);
    String reason = argvar(args, 1, "");
    Optional<BanModel> ban = bans.findById(id);

    if (reason.isBlank())
      reason = null;

    if (ban.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.BAN_UNKNOWN)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    BanModel revoked = bans.revokeBan(ban.get(), p, reason);

    if (revoked == null) {
      p.sendMessage(
        cfg.get(ConfigKey.BAN_ALREADY_REVOKED)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    bans.broadcastRevoke(revoked);
  }
}
