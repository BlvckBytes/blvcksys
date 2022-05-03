package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Feed yourself or an other player instantly.
 */
@AutoConstruct
public class FeedCommand extends APlayerCommand {

  public FeedCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "feed",
      "Feed yourself or others",
      PlayerPermission.FEED,
      new CommandArgument("[player]", "The player to feed", PlayerPermission.FEED_OTHERS)
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest online players
    if (currArg == 0)
      return suggestOnlinePlayers(p, args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0, p);
    boolean isSelf = target.equals(p);

    // Apply the food level increase
    int before = target.getFoodLevel();
    target.setFoodLevel(20);

    // Calculate the delta
    int delta = 20 - before;
    String deltaStr = (delta >= 0 ? "+" : "") + delta;

    // Inform target
    target.sendMessage(
      cfg.get(isSelf ? ConfigKey.FEED_SELF : ConfigKey.FEED_OTHERS_RECEIVER)
        .withPrefix()
        .withVariable("issuer", p.getName())
        .withVariable("delta", deltaStr)
        .asScalar()
    );

    // Inform sender
    if (!isSelf)
      p.sendMessage(
        cfg.get(ConfigKey.FEED_OTHERS_SENDER)
          .withPrefix()
          .withVariable("target", target.getName())
          .withVariable("delta", deltaStr)
          .asScalar()
      );
  }
}
