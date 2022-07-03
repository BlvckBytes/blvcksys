package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IMuteHandler;
import me.blvckbytes.blvcksys.persistence.models.MuteModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Cast a new mute upon a player.
*/
@AutoConstruct
public class MuteCommand extends APlayerCommand {

  private final IMuteHandler mutes;

  public MuteCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IMuteHandler mutes
  ) {
    super(
      plugin, logger, cfg, refl,
      "mute",
      "Cast a new mute upon a player",
      PlayerPermission.COMMAND_MUTE.toString(),
      new CommandArgument("<name>", "Name of the target player"),
      new CommandArgument("<duration>", "Duration of this punishment"),
      new CommandArgument("[reason]", "Reason of this punishment")
    );

    this.mutes = mutes;
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

    Optional<MuteModel> existing = mutes.isCurrentlyMuted(target);
    if (existing.isPresent()) {
      p.sendMessage(
        cfg.get(ConfigKey.MUTE_ALREADY_MUTED)
          .withVariable("target", target.getName())
          .asScalar()
      );
      return;
    }

    int duration = parseDuration(args, 1, null);
    String reason = argvar(args, 2, "");

    mutes.broadcastMute(mutes.createMute(p, target, duration, reason));
  }
}
