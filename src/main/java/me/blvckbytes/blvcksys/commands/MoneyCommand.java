package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
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
  Created On: 05/13/2022

  Manage a player's money by setting (overwriting), adding, removing
  or getting (reading) their coins.
 */
@AutoConstruct
public class MoneyCommand extends APlayerCommand {

  /**
   * Represents the action an executor can perform on a target's amount of money
   */
  private enum MoneyAction {
    GET,      // Get the current amount of money
    GIVE,     // Give money (adds)
    REMOVE,   // Remove money (subtracts)
    SET       // Sets money
  }

  private final IPlayerStatsHandler stats;

  public MoneyCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPlayerStatsHandler stats
  ) {
    super(
      plugin, logger, cfg, refl,
      "money",
      "Add, remove or set the amount of money of players",
      PlayerPermission.COMMAND_MONEY,
      new CommandArgument("<action>", "Action to perform"),
      new CommandArgument("[player]", "Target player"),
      new CommandArgument("[amount]", "Amount of money")
    );

    this.stats = stats;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest actions
    if (currArg == 0)
      return suggestEnum(args, currArg, MoneyAction.class);

    // Suggest all players
    if (currArg == 1)
      return suggestOfflinePlayers(args, currArg);

    // Suggest placeholder
    if (currArg == 2)
      return Stream.of(getArgumentPlaceholder(currArg));

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    MoneyAction action = parseEnum(MoneyAction.class, args, 0, null);
    OfflinePlayer target = offlinePlayer(args, 1);
    boolean isSelf = target.equals(p);

    // Foreign target (not self)
    if (!isSelf)
      ensurePermission(p, PlayerPermission.COMMAND_MONEY_OTHERS);

    int before = stats.getStats(target).getMoney();

    // Get - read the current number of coins
    if (action == MoneyAction.GET) {
      p.sendMessage(
        cfg.get(isSelf ? ConfigKey.MONEY_GET_SELF : ConfigKey.MONEY_GET_OTHERS)
          .withPrefix()
          .withVariable("target", target.getName())
          .withVariable("money", before)
          .asScalar()
      );
      return;
    }

    // Set, remove, give: Override, subtract or add a given amount of coins
    int amount = parseInt(args, 2);

    // Decide on how to affect the amount of coins
    int after = before;
    switch (action) {
      case SET -> after = amount;
      case GIVE -> after += amount;
      case REMOVE -> after = Math.max(0, after - amount);
    }

    // Format a delta string
    int delta = after - before;
    String deltaStr = (delta >= 0 ? "+" : "") + delta;

    // Apply the change
    stats.addMoney(target, delta);

    // Notify the issuer
    p.sendMessage(
      cfg.get(isSelf ? ConfigKey.MONEY_SET_SELF : ConfigKey.MONEY_SET_OTHERS_SENDER)
        .withPrefix()
        .withVariable("target", target.getName())
        .withVariable("money", after)
        .withVariable("delta", deltaStr)
        .asScalar()
    );

    // Not self, notify the receiver
    if (!isSelf && target.isOnline())
      ((Player) target).sendMessage(
        cfg.get(ConfigKey.MONEY_SET_OTHERS_RECEIVER)
          .withPrefix()
          .withVariable("issuer", p.getName())
          .withVariable("money", after)
          .withVariable("delta", deltaStr)
          .asScalar()
      );
  }
}
