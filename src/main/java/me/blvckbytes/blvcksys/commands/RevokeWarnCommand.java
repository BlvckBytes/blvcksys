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
  Created On: 05/17/2022

  Revoke an existing, previously casted warn.
*/
@AutoConstruct
public class RevokeWarnCommand extends APlayerCommand {

  private final IWarnHandler warns;
  private final IPersistence pers;

  public RevokeWarnCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IWarnHandler warns,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "revokewarn",
      "Revoke a previously casted warn",
      PlayerPermission.COMMAND_WARNREVOKE,
      new CommandArgument("<id>", "ID of the warn"),
      new CommandArgument("[reason]", "Reason of this revocation")
    );

    this.warns = warns;
    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all possible UUIDs
    if (currArg == 0)
      return suggestModels(args, currArg, WarnModel.class, "id", pers);

    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    UUID id = parseUUID(args, 0);
    String reason = argvar(args, 1, "");
    Optional<WarnModel> warn = warns.findById(id);

    if (reason.isBlank())
      reason = null;

    if (warn.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.WARN_UNKNOWN)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    if (!warn.get().isActive()) {
      p.sendMessage(
        cfg.get(ConfigKey.WARN_NOT_ACTIVE)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    WarnModel revoked = warns.revokeWarn(warn.get(), p, reason);

    if (revoked == null) {
      p.sendMessage(
        cfg.get(ConfigKey.WARN_ALREADY_REVOKED)
          .withPrefix()
          .withVariable("id", id)
          .asScalar()
      );
      return;
    }

    warns.broadcastRevoke(revoked);
  }
}
