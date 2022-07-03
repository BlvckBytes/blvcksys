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
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Give a certain amount of a specified material either to the executor
  or another target. Items are added directly into the inventory and
  stacked with other compatible items as efficiently as possible. Whenever
  the inventory doesn't provide enough space for the new items, they
  will be dropped at the player's head-location.
 */
@AutoConstruct
public class GiveCommand extends APlayerCommand implements IGiveCommand {

  // Which of the materials cannot be chosen from the enum's constants
  private static final List<Material> BANNED_MATERIALS = List.of(Material.AIR);

  public GiveCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "give",
      "Give a certain amount of an item to yourself or others",
      PlayerPermission.COMMAND_GIVE.toString(),
      new CommandArgument("<item>", "Material of the item"),
      new CommandArgument("<amount>", "Number of items"),
      new CommandArgument("[player/all]", "The receiving player", PlayerPermission.COMMAND_GIVE_OTHERS.toString())
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // First argument - provide all material enum values (excluding banned values)
    if (currArg == 0)
      return suggestEnum(args, currArg, Material.class, (acc, curr) -> {
        if (!BANNED_MATERIALS.contains(curr))
          acc.add(curr.toString());
      });

    // Provide placeholder for the amount
    else if (currArg == 1)
      return Stream.of(getArgumentPlaceholder(currArg));

      // Third argument - provide all online players or "all"
    else if (currArg == 2)
      return suggestOnlinePlayers(p, args, currArg, true);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    // Parse the material
    Material mat = parseEnum(Material.class, args, 0, null, (repr, con) -> con.name().equalsIgnoreCase(repr));

    // Parse the amount
    int amount = parseInt(args, 1);

    // Give to all players
    if (argval(args, 2, "self").equalsIgnoreCase("all")) {
      ensurePermission(p, PlayerPermission.COMMAND_GIVE_ALL.toString());

      for (Player target : Bukkit.getOnlinePlayers())
        // Skip self
        if (target != p)
          giveItem(p, target, mat, amount, true);

      // Notify the dispatcher
      p.sendMessage(
        cfg.get(ConfigKey.GIVE_ALL_SENDER)
          .withPrefix()
          .withVariable("amount", amount)
          .withVariable("material", mat)
          .asScalar()
      );
      return;
    }

    // Assume the target to be the dispatcher
    Player target = onlinePlayer(args, 2, p);
    giveItem(p, target, mat, amount, false);

    if (target != p)
      p.sendMessage(
        cfg.get(ConfigKey.GIVE_SENDER)
          .withPrefix()
          .withVariable("target", target.getDisplayName())
          .withVariable("amount", amount)
          .withVariable("material", mat)
          .asScalar()
      );

    // Notify self give
    else
      p.sendMessage(
        cfg.get(ConfigKey.GIVE_SELF)
          .withPrefix()
          .withVariable("amount", amount)
          .withVariable("material", mat)
          .asScalar()
      );
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  private void giveItem(Player sender, Player receiver, Material mat, int amount, boolean isAll) {
    // Hand out the items
    int dropped = giveItemsOrDrop(receiver, new ItemStack(mat, amount));

    String dropMsg = cfg.get(ConfigKey.GIVE_DROPPED)
      .withPrefix()
      .withVariable("num_dropped", dropped)
      .asScalar();

    // Notify the executor about the drop (if it's not an "all" giveaway)
    if (dropped > 0 && !isAll)
      sender.sendMessage(dropMsg);

    // Notify about giveaway (not to self)
    if (receiver != sender) {

      receiver.sendMessage(
        cfg.get(isAll ? ConfigKey.GIVE_ALL_RECEIVER : ConfigKey.GIVE_RECEIVER)
          .withPrefix()
          .withVariable("issuer", sender.getDisplayName())
          .withVariable("amount", amount)
          .withVariable("material", mat)
          .asScalar()
      );

      // Notify the target about the drop
      if (dropped > 0)
        receiver.sendMessage(dropMsg);
    }
  }

  /**
   * Add the given items to a player's inventory as much as possible and
   * return the count of items that didn't fit
   * @param target Target player
   * @param item Item to add
   * @return Number of items that didn't fit
   */
  private int addToInventory(Player target, ItemStack item) {
    // This number will be decremented as space is found along the way
    int remaining = item.getAmount();
    int stackSize = item.getType().getMaxStackSize();

    // Iterate all slots
    ItemStack[] contents = target.getInventory().getStorageContents();
    for (int i = 0; i < contents.length; i++) {
      ItemStack stack = contents[i];

      // Done, no more items remaining
      if (remaining < 0)
        break;

      // Completely vacant slot
      if (stack == null || stack.getType() == Material.AIR) {
        ItemStack remItems = item.clone();

        // Set as many items as possible or as many as remain
        int num = Math.min(remaining, stackSize);
        remItems.setAmount(num);

        target.getInventory().setItem(i, remItems);
        remaining -= num;
        continue;
      }

      // Incompatible stacks, ignore
      if (!stack.isSimilar(item))
        continue;

      // Compatible stack but no more room left
      int usable = Math.max(0, stackSize - stack.getAmount());
      if (usable == 0)
        continue;

      // Add the last few remaining items, done
      if (usable >= remaining) {
        stack.setAmount(stack.getAmount() + remaining);
        remaining = 0;
        break;
      }

      // Set to a full stack and subtract the delta from remaining
      stack.setAmount(stackSize);
      remaining -= usable;
    }

    // Return remaining items that didn't fit
    return remaining;
  }

  @Override
  public int giveItemsOrDrop(Player target, ItemStack stack) {
    // Add as much as possible into the inventory
    int remaining = addToInventory(target, stack);
    int dropped = remaining;

    // Done, everything fit
    if (remaining == 0)
      return 0;

    // Could not get the world, all further iterations make no sense
    World w = target.getLocation().getWorld();
    if (w == null)
      return 0;

    // Drop all remaining items
    int stackSize = stack.getMaxStackSize();
    while (remaining > 0) {
      ItemStack items = stack.clone();
      items.setAmount(Math.min(remaining, stackSize));
      w.dropItemNaturally(target.getEyeLocation(), items);
      remaining -= items.getAmount();
    }

    return dropped;
  }
}
