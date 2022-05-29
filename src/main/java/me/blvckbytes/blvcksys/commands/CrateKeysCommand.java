package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.ICrateHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.CrateKeyModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.TidyTable;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Manage a player's crate keys by setting (overwriting), adding, removing
  or getting (reading) their keys.
*/
@AutoConstruct
public class CrateKeysCommand extends APlayerCommand {

  private enum CrateKeyAction {
    GET,      // Get the current amount of keys
    GIVE,     // Give keys (adds)
    REMOVE,   // Remove keys (subtracts)
    SET       // Sets keys
  }

  private final IPersistence pers;
  private final ICrateHandler crateHandler;
  private final IFontWidthTable fwTable;

  public CrateKeysCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ICrateHandler crateHandler,
    @AutoInject IPersistence pers,
    @AutoInject IFontWidthTable fwTable
  ) {
    super(
      plugin, logger, cfg, refl,
      "cratekeys",
      "Manage crate keys",
      null,
      new CommandArgument("<action>", "Action to perform", PlayerPermission.COMMAND_CRATEKEYS_MANAGE),
      new CommandArgument("[player]", "Target player"),
      new CommandArgument("[crate]", "Target crate"),
      new CommandArgument("[amount]", "Amount of keys")
    );

    this.pers = pers;
    this.crateHandler = crateHandler;
    this.fwTable = fwTable;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestEnum(args, currArg, CrateKeyAction.class);
    if (currArg == 1)
      return suggestOfflinePlayers(args, currArg);
    if (currArg == 2)
      return suggestModels(args, currArg, CrateModel.class, "name", pers);
    if (currArg == 3)
      return Stream.of(getArgumentPlaceholder(currArg));
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    CrateKeyAction action = parseEnum(CrateKeyAction.class, args, 0, CrateKeyAction.GET);
    OfflinePlayer target = offlinePlayer(args, 1, p);
    boolean isSelf = target.equals(p);

    if (action == CrateKeyAction.GET) {
      List<CrateKeyModel> keys = crateHandler.getAllKeys(target);

      TidyTable table = new TidyTable("|", fwTable);

      table.addLines(
        cfg.get(isSelf ? ConfigKey.COMMAND_CRATEKEYS_LIST_HEADER_SELF : ConfigKey.COMMAND_CRATEKEYS_LIST_HEADER_OTHERS)
          .withPrefixes()
          .withVariable("target", target.getName())
          .asScalar()
      );

      for (CrateKeyModel key : keys) {
        CrateModel crate = crateHandler.getCrate(key.getCrateId()).orElse(null);

        if (crate == null)
          continue;

        table.addLine(
          cfg.get(ConfigKey.COMMAND_CRATEKEYS_LIST_KEY_FORMAT)
            .withPrefix()
            .withVariable("crate", crate.getName())
            .withVariable("keys", key.getNumberOfKeys())
            .asScalar()
        );
      }

      table.displayTo(p);
      return;
    }

    // Set, remove, give: Override, subtract or add a given number of keys

    String crate = argval(args, 2);
    int amount = parseInt(args, 3);
    CrateKeyModel keys = crateHandler.getKeys(target, crate).orElse(null);

    if (keys == null) {
      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_CRATEKEYS_SET_NOT_FOUND)
          .withPrefix()
          .withVariable("crate", crate)
          .asScalar()
      );
      return;
    }

    int before = keys.getNumberOfKeys();

    // Decide on how to affect the number of keys
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
    crateHandler.updateKeys(target, crate, after);

    // Notify the issuer
    p.sendMessage(
      cfg.get(isSelf ? ConfigKey.COMMAND_CRATEKEYS_SET_SELF : ConfigKey.COMMAND_CRATEKEYS_SET_OTHERS_SENDER)
        .withPrefix()
        .withVariable("target", target.getName())
        .withVariable("crate", crate)
        .withVariable("keys", after)
        .withVariable("delta", deltaStr)
        .asScalar()
    );

    // Not self, notify the receiver
    if (!isSelf && target instanceof Player online)
      online.sendMessage(
        cfg.get(ConfigKey.COMMAND_CRATEKEYS_SET_OTHERS_RECEIVER)
          .withPrefix()
          .withVariable("issuer", p.getName())
          .withVariable("crate", crate)
          .withVariable("keys", after)
          .withVariable("delta", deltaStr)
          .asScalar()
      );
  }
}
