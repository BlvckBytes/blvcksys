package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Clear your own or another inventory and have a confirmation to
  avoid accidental losses.
*/
@AutoConstruct
public class ClearInventoryCommand extends APlayerCommand {

  private final ChatUtil chat;

  public ClearInventoryCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ChatUtil chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "clearinventory,ci",
      "Clear the inventory for yourself or others",
      PlayerPermission.COMMAND_CLEARINVENTORY_SELF.toString(),
      new CommandArgument("[player]", "Player to clear the inventory for", PlayerPermission.COMMAND_CLEARINVENTORY_OTHERS.toString())
    );

    this.chat = chat;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all online players
    if (currArg == 0)
      return suggestOnlinePlayers(p, args, currArg, false);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0, p);
    boolean isSelf = target == p;

    // Send confirmation prompt
    chat.beginPrompt(
      p, null,
      cfg.get(isSelf ? ConfigKey.CLEARINVENTORY_CONFIRM_SELF : ConfigKey.CLEARINVENTORY_CONFIRM_OTHERS)
        .withPrefix()
        .withVariable("target", target.getName()),
      cfg.get(ConfigKey.CHATBUTTONS_EXPIRED)
        .withPrefix(),
      List.of(
        new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_YES), null, () -> clearInventory(p, target, isSelf)),
        new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_NO), null, () -> {
          p.sendMessage(
            cfg.get(ConfigKey.CLEARINVENTORY_CANCELLED)
              .withPrefix()
              .asScalar()
          );
        })
      )
    );
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Clear the inventory of the target player and inform all involved players
   * @param issuer Player issuing the clear
   * @param target Player to be cleared
   * @param isSelf Whether the issuer clears their own inventory
   */
  private void clearInventory(Player issuer, Player target, boolean isSelf) {
    // Inform the issuer
    issuer.sendMessage(
      cfg.get(isSelf ? ConfigKey.CLEARINVENTORY_CLEARED_SELF : ConfigKey.CLEARINVENTORY_CLEARED_SENDER)
        .withPrefix()
        .withVariable("target", target.getName())
        .asScalar()
    );

    // Cleared another inventory
    if (issuer != target) {
      // Inform the target
      target.sendMessage(
        cfg.get(ConfigKey.CLEARINVENTORY_CLEARED_TARGET)
          .withPrefix()
          .withVariable("issuer", issuer.getName())
          .asScalar()
      );
    }

    // Actually clear the inventory
    target.getInventory().clear();
  }
}
