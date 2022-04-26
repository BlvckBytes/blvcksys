package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.cmd.exception.CommandException;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Stream;

@AutoConstruct
public class GiveCommand extends APlayerCommand {

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
    // First argument - provide all material enum values (excluding air)
    if (currArg == 0)
      return suggestEnum(args, currArg, Material.class, BANNED_MATERIALS);

    // Provide placeholder for the amount
    else if (currArg == 1)
      return Stream.of(getArgumentPlaceholder(currArg));

      // Third argument - provide all online players
    else if (currArg == 2)
      return suggestOnlinePlayers(args, currArg);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    // Parse the material
    Material mat = parseEnum(Material.class, args, 0, BANNED_MATERIALS);

    // Parse the amount
    int amount = parseInt(args, 1);

    // Assume the target to be the dispatcher
    Player target = onlinePlayer(args, 2, p);

    // Hand out the items
    int dropped = giveItems(target, mat, amount);
    String dropMsg = cfg.get(ConfigKey.GIVE_DROPPED)
      .withPrefix()
      .withVariable("num_dropped", dropped)
      .asScalar();

    // Notify the executor about the drop
    if (dropped > 0)
      p.sendMessage(dropMsg);

    // Notify about giveaway
    if (target != p) {
      p.sendMessage(
        cfg.get(ConfigKey.GIVE_SENDER)
          .withPrefix()
          .withVariable("target", target.getDisplayName())
          .withVariable("amount", amount)
          .withVariable("material", mat)
          .asScalar()
      );

      target.sendMessage(
        cfg.get(ConfigKey.GIVE_RECEIVER)
          .withPrefix()
          .withVariable("executor", p.getDisplayName())
          .withVariable("amount", amount)
          .withVariable("material", mat)
          .asScalar()
      );

      // Notify the target about the drop
      if (dropped > 0)
        target.sendMessage(dropMsg);
    }

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
