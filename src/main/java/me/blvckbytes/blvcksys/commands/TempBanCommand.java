package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IBanHandler;
import me.blvckbytes.blvcksys.persistence.models.BanModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/13/2022

  Cast a new temporary ban upon a player.
*/
@AutoConstruct
public class TempBanCommand extends APlayerCommand {

  private final IBanHandler bans;

  public TempBanCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IBanHandler bans
  ) {
    super(
      plugin, logger, cfg, refl,
      "tempban",
      "Cast a new temporary ban upon a player",
      PlayerPermission.COMMAND_TEMPBAN.toString(),
      new CommandArgument("<name>", "Name of the target player"),
      new CommandArgument("<duration>", "Duration of this punishment"),
      new CommandArgument("[reason]", "Reason of this punishment")
    );

    this.bans = bans;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOfflinePlayers(args, currArg);

    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    OfflinePlayer target = offlinePlayer(args, 0);

    Optional<BanModel> existing = bans.isCurrentlyBanned(target, null);
    if (existing.isPresent()) {
      p.sendMessage(
        cfg.get(ConfigKey.BAN_ALREADY_BANNED)
          .withVariable("target", target.getName())
          .asScalar()
      );
      return;
    }

    int duration = parseDuration(args, 1, null);
    String reason = argvar(args, 2, "");

    bans.broadcastBan(bans.createBan(p, target, duration, null, reason));
  }
}
