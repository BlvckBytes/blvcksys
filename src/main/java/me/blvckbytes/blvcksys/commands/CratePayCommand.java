package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.ICrateHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.CrateKeyModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Pay some of your own crate keys to another player.
*/
@AutoConstruct
public class CratePayCommand extends APlayerCommand {

  private final IPersistence pers;
  private final ICrateHandler crateHandler;

  public CratePayCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ICrateHandler crateHandler,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "cratepay",
      "Pay crate keys to another player",
      null,
      new CommandArgument("<crate>", "Target crate"),
      new CommandArgument("<player>", "Target player"),
      new CommandArgument("<amount>", "Amount to pay")
    );

    this.pers = pers;
    this.crateHandler = crateHandler;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestModels(args, currArg, CrateModel.class, "name", pers);
    if (currArg == 1)
      return suggestOfflinePlayers(args, currArg);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String crate = argval(args, 0);
    OfflinePlayer target = offlinePlayer(args, 1);
    int amount = parseInt(args, 2);

    if (target.equals(p)) {
      p.sendMessage(
        cfg.get(ConfigKey.CRATEPAY_SELF)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    if (amount <= 0) {
      p.sendMessage(
        cfg.get(ConfigKey.CRATEPAY_INVALID_AMOUNT)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    CrateKeyModel senderKeys = crateHandler.getKeys(p, crate).orElse(null);
    CrateKeyModel receiverKeys = crateHandler.getKeys(target, crate).orElse(null);

    if (senderKeys == null || receiverKeys == null) {
      p.sendMessage(
        cfg.get(ConfigKey.CRATEPAY_NOT_FOUND)
          .withPrefix()
          .withVariable("crate", crate)
          .asScalar()
      );
      return;
    }

    if (senderKeys.getNumberOfKeys() == 0) {
      p.sendMessage(
        cfg.get(ConfigKey.CRATEPAY_HAS_NONE)
          .withPrefix()
          .withVariable("crate", crate)
          .asScalar()
      );
      return;
    }

    if (amount > senderKeys.getNumberOfKeys()) {
      p.sendMessage(
        cfg.get(ConfigKey.CRATEPAY_TOO_MUCH)
          .withPrefix()
          .withVariable("available", senderKeys.getNumberOfKeys())
          .withVariable("crate", crate)
          .asScalar()
      );
      return;
    }

    crateHandler.updateKeys(p, crate, senderKeys.getNumberOfKeys() - amount);
    crateHandler.updateKeys(target, crate, receiverKeys.getNumberOfKeys() + amount);

    p.sendMessage(
      cfg.get(ConfigKey.CRATEPAY_TRANSFERED_SENDER)
        .withPrefix()
        .withVariable("target", target.getName())
        .withVariable("crate", crate)
        .withVariable("keys", amount)
        .asScalar()
    );

    if (target.isOnline()) {
      ((Player) target).sendMessage(
        cfg.get(ConfigKey.CRATEPAY_TRANSFERED_RECEIVER)
          .withPrefix()
          .withVariable("keys", amount)
          .withVariable("crate", crate)
          .withVariable("sender", p.getName())
          .asScalar()
      );
    }
  }
}
