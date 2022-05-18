package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerStatsHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  Pay some of your own money to another player.
*/
@AutoConstruct
public class PayCommand extends APlayerCommand {

  private final IPlayerStatsHandler stats;

  public PayCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPlayerStatsHandler stats
  ) {
    super(
      plugin, logger, cfg, refl,
      "pay",
      "Pay some of your money to another player",
      null,
      new CommandArgument("<player>", "Player to pay to"),
      new CommandArgument("<amount>", "Amount to pay")
    );

    this.stats = stats;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOfflinePlayers(args, currArg);
    if (currArg == 1)
      return Stream.of(getArgumentPlaceholder(currArg));
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    OfflinePlayer target = offlinePlayer(args, 0, p);
    int amount = parseInt(args, 1);

    if (target.equals(p)) {
      p.sendMessage(
        cfg.get(ConfigKey.PAY_SELF)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    if (amount <= 0) {
      p.sendMessage(
        cfg.get(ConfigKey.PAY_INVALID_AMOUNT)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    int available = stats.getMoney(p);

    if (available == 0) {
      p.sendMessage(
        cfg.get(ConfigKey.PAY_HAS_NONE)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    if (amount > available) {
      p.sendMessage(
        cfg.get(ConfigKey.PAY_TOO_MUCH)
          .withPrefix()
          .withVariable("available", available)
          .asScalar()
      );
      return;
    }

    stats.setMoney(p, available - amount);
    stats.setMoney(target, stats.getMoney(target) + amount);

    p.sendMessage(
      cfg.get(ConfigKey.PAY_TRANSFERED_SENDER)
        .withPrefix()
        .withVariable("target", target.getName())
        .withVariable("amount", amount)
        .asScalar()
    );

    if (target instanceof Player online) {
      online.sendMessage(
        cfg.get(ConfigKey.PAY_TRANSFERED_RECEIVER)
          .withPrefix()
          .withVariable("amount", amount)
          .withVariable("sender", p.getName())
          .asScalar()
      );
    }
  }
}
