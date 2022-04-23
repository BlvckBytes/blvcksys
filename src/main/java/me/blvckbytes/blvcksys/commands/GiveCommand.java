package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.config.Config;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.cmd.CommandResult;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.stream.Stream;

@AutoConstruct
public class GiveCommand extends APlayerCommand {

  public GiveCommand() {
    super(
      "give",
      "Give a certain amount of an item to yourself or others",
      new String[][] {
        { "<item>", "Material of the item" },
        { "<amount>", "Number of items" },
        { "[player]", "The receiving player" }
      }
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // First argument - provide all material enum values
    if (currArg == 0)
      return Arrays.stream(Material.values())
        .map(Enum::toString)
        .filter(m -> m.toLowerCase().contains(args[currArg].toLowerCase()));

    // Third argument - provide all online players
    else if (currArg == 2)
      return Bukkit.getOnlinePlayers()
        .stream()
        .map(Player::getDisplayName)
        .filter(n -> n.toLowerCase().contains(args[currArg].toLowerCase()));

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected CommandResult onInvocation(Player p, String label, String[] args) {
    if (args.length < 2 || args.length > 3)
      return usageMismatch();

    // Try to parse the material
    String matstr = args[0].toUpperCase();
    Material mat = Material.getMaterial(matstr);

    // Unknown material requested, provide proper help
    if (mat == null)
      return customError(Config.getP(ConfigKey.GIVE_INVALID_ITEM, matstr));

    // Try to parse the amount
    MutableInt amount = new MutableInt(0);
    CommandResult res = parseInt(args[1], amount);

    // Not an integer
    if (res != null)
      return res;

    // Assume the target to be the dispatcher
    Player target = p;

    // Use the optional player argument if it's provided
    if (args.length == 3)
      target = Bukkit.getPlayer(args[2]);

    // Player not online
    if (target == null)
      return playerOffline(args[2]);

    // Hand out the items
    int dropped = giveItems(target, mat, amount.intValue());
    String dropMsg = Config.getP(ConfigKey.GIVE_DROPPED, dropped);

    // Notify the executor about the drop
    if (dropped > 0)
      p.sendMessage(dropMsg);

    // Notify about giveaway
    if (target != p) {
      p.sendMessage(Config.getP(ConfigKey.GIVE_SENDER, target.getDisplayName(), amount.intValue(), mat.toString()));
      target.sendMessage(Config.getP(ConfigKey.GIVE_RECEIVER, p.getDisplayName(), amount.intValue(), mat.toString()));

      // Notify the target about the drop
      if (dropped > 0)
        target.sendMessage(dropMsg);
    }

    // Notify self give
    else
      p.sendMessage(Config.getP(ConfigKey.GIVE_SELF, amount.intValue(), mat.toString()));

    return success();
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  private int addToInventory(Player target, Material mat, int amount) {
    // This number will be decremented as space is found along the way
    int remaining = amount;
    int stackSize = mat.getMaxStackSize();

    // Iterate all slots
    ItemStack[] contents = target.getInventory().getStorageContents();
    for (int i = 0; i < contents.length; i++) {
      ItemStack stack = contents[i];

      // Done, no more items remaining
      if (remaining < 0)
        break;

      // Completely vacant slot
      if (stack == null || stack.getType() == Material.AIR) {
        // Set as many items as possible or as many as remain
        int num = Math.min(remaining, stackSize);
        target.getInventory().setItem(i, new ItemStack(mat, num));
        remaining -= num;
        continue;
      }

      // Different type, ignore
      if (stack.getType() != mat)
        continue;

      // Same type but no more room left
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

  private int giveItems(Player target, Material mat, int amount) {
    // Add as much as possible into the inventory
    int remaining = addToInventory(target, mat, amount);
    int dropped = remaining;

    // Done, everything fit
    if (remaining == 0)
      return 0;

    // Could not get the world, all further iterations make no sense
    World w = target.getLocation().getWorld();
    if (w == null)
      return 0;

    // Drop all remaining items
    int stackSize = mat.getMaxStackSize();
    while (remaining > 0) {
      ItemStack items = new ItemStack(mat, Math.min(remaining, stackSize));
      w.dropItemNaturally(target.getEyeLocation(), items);
      remaining -= items.getAmount();
    }

    return dropped;
  }
}
