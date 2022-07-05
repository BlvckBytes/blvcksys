package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IWarnHandler;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Completely clear all warns of a player.
*/
@AutoConstruct
public class ClearWarnsCommand extends APlayerCommand {

  private final IWarnHandler warns;
  private final ChatUtil chat;

  public ClearWarnsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IWarnHandler warns,
    @AutoInject ChatUtil chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "clearwarns",
      "Clear all warns of a specific player",
      PlayerPermission.COMMAND_CLEARWARNS.toString(),
      new CommandArgument("<player>", "Player to clear the warns from")
    );

    this.warns = warns;
    this.chat = chat;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOfflinePlayers(args, currArg);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    OfflinePlayer target = offlinePlayer(args, 0);

    int numWarns = warns.countAllWarns(target);

    if (numWarns == 0) {
      p.sendMessage(
        cfg.get(ConfigKey.WARN_CLEAR_NO_WARNS)
          .withPrefix()
          .withVariable("target", target.getName())
          .asScalar()
      );
      return;
    }

    chat.beginPrompt(
      p, null,
      cfg.get(ConfigKey.WARN_CLEAR_CONFIRMATION)
        .withPrefix()
        .withVariable("target", target.getName())
        .withVariable("num_warns", numWarns),
      cfg.get(ConfigKey.CHATBUTTONS_EXPIRED).withPrefix(),
      List.of(
        new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_YES), null, () -> {
          warns.clearWarns(target);
          warns.broadcastClear(target, numWarns);
        }),
        new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_NO), null, () -> {
          p.sendMessage(
            cfg.get(ConfigKey.WARN_CLEAR_CANCELLED)
              .withPrefix()
              .asScalar()
          );
        })
      )
    );
  }
}
