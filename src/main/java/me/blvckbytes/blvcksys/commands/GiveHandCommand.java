package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Gives the item currently held in the hand to either a specific receiving
  player or to all online players.
*/
@AutoConstruct
public class GiveHandCommand extends APlayerCommand {

  private final IGiveCommand give;

  public GiveHandCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IGiveCommand give
  ) {
    super(
      plugin, logger, cfg, refl,
      "givehand, gh",
      "Give the item in your hand away",
      PlayerPermission.COMMAND_GIVEHAND.toString(),
      new CommandArgument("<player/all>", "Who to give this item to")
    );

    this.give = give;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest online players or "all"
    if (currArg == 0)
      return suggestOnlinePlayers(p, args, currArg, true);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    ItemStack stack = p.getInventory().getItemInMainHand();

    // Has to have something in their hand
    if (stack.getType() == Material.AIR) {
      p.sendMessage(
        cfg.get(ConfigKey.GIVEHAND_NOITEM)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Figure out what name to display
    ItemMeta meta = stack.getItemMeta();
    String itemName = (meta == null || meta.getDisplayName().isBlank()) ? stack.getType().toString() : meta.getDisplayName();

    // Give this item to all online players
    if (argval(args, 0).equalsIgnoreCase("all")) {
      ensurePermission(p, PlayerPermission.COMMAND_GIVEHAND_ALL.toString());

      for (Player target : Bukkit.getOnlinePlayers()) {
        // Skip self
        if (target != p)
          giveItem(p, target, stack, itemName, true);
      }

      // Inform the dispatcher
      p.sendMessage(
        cfg.get(ConfigKey.GIVEHAND_ALL_SENDER)
          .withPrefix()
          .withVariable("item_name", itemName)
          .asScalar()
      );

      return;
    }

    // Specific target player
    Player target = onlinePlayer(args, 0);
    giveItem(p, target, stack, itemName, false);

    // Inform the dispatcher
    p.sendMessage(
      cfg.get(ConfigKey.GIVEHAND_SPECIFIC_SENDER)
        .withPrefix()
        .withVariable("item_name", itemName)
        .withVariable("receiver", target.getName())
        .asScalar()
    );
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Give an item to a specific receiving player
   * @param sender Item sender (dispatcher)
   * @param receiver Receiving player
   * @param item Item to give
   * @param itemName Name to display for this item
   * @param isAll Whether this giveaway is targetting all online players
   */
  private void giveItem(Player sender, Player receiver, ItemStack item, String itemName, boolean isAll) {
    // Inform the receiver
    receiver.sendMessage(
      cfg.get(isAll ? ConfigKey.GIVEHAND_ALL_RECEIVER : ConfigKey.GIVEHAND_SPECIFIC_RECEIVER)
        .withPrefix()
        .withVariable("item_name", itemName)
        .withVariable("issuer", sender.getName())
        .asScalar()
      );

    // Give away the actual item safely
    int dropped = give.giveItemsOrDrop(receiver, item);

    // Notify the receiver about the drop
    if (dropped > 0)
      receiver.sendMessage(
        cfg.get(ConfigKey.GIVE_DROPPED)
        .withPrefix()
        .withVariable("num_dropped", dropped)
        .asScalar()
      );
  }
}
