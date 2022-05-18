package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IIgnoreHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/19/2022

  Allows players ignore other players on different
  channels of communication.
*/
@AutoConstruct
public class IgnoreCommand extends APlayerCommand {

  private enum CommunicationChannel {
    MSG,
    CHAT
  }

  private final IIgnoreHandler ignores;

  public IgnoreCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IIgnoreHandler ignores
  ) {
    super(
      plugin, logger, cfg, refl,
      "ignore",
      "Ignore another player",
      null,
      new CommandArgument("<player>", "Player to ignore"),
      new CommandArgument("<type>", "Type of communication channel")
    );

    this.ignores = ignores;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOfflinePlayers(args, currArg);
    if (currArg == 1)
      return suggestEnum(args, currArg, CommunicationChannel.class);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    OfflinePlayer target = offlinePlayer(args, 0);
    CommunicationChannel ch = parseEnum(CommunicationChannel.class, args, 1, null);

    if (target.equals(p)) {
      p.sendMessage(
        cfg.get(ConfigKey.IGNORE_SELF)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    if (ch == CommunicationChannel.MSG) {
      boolean value = !ignores.getMsgIgnore(p, target);
      ignores.setMsgIgnore(p, target, value);

      p.sendMessage(
        cfg.get(value ? ConfigKey.IGNORE_MSG_ENABLED : ConfigKey.IGNORE_MSG_DISABLED)
          .withPrefix()
          .withVariable("target", target.getName())
          .asScalar()
      );
      return;
    }

    if (ch == CommunicationChannel.CHAT) {
      boolean value = !ignores.getChatIgnore(p, target);
      ignores.setChatIgnore(p, target, value);

      p.sendMessage(
        cfg.get(value ? ConfigKey.IGNORE_CHAT_ENABLED : ConfigKey.IGNORE_CHAT_DISABLED)
          .withPrefix()
          .withVariable("target", target.getName())
          .asScalar()
      );
      return;
    }
  }
}
