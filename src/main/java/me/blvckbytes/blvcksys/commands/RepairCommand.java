package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.handlers.ICooldownHandler;
import me.blvckbytes.blvcksys.handlers.ICooldownable;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Repairs the inventory held in the main hand, or the whole inventory.
*/
@AutoConstruct
public class RepairCommand extends APlayerCommand {

  // Cooldown token for the repair cooldown
  private static final String CT_REPAIR = "cmd_repair";

  // Cooldown duration for the repair command in seconds
  private static final int CD_REPAIR = 60 * 5;

  private final ICooldownHandler cooldownHandler;

  public RepairCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ICooldownHandler cooldownHandler
  ) {
    super(
      plugin, logger, cfg, refl,
      "repair",
      "Repair used items",
      PlayerPermission.COMMAND_REPAIR.toString(),
      new CommandArgument("[all]", "Repair all items in your inventory", PlayerPermission.COMMAND_REPAIR_ALL.toString())
    );

    this.cooldownHandler = cooldownHandler;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return Stream.of("all");
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    boolean repairAll = argval(args, 0, "hand").equalsIgnoreCase("all");

    // Repair the whole inventory
    if (repairAll) {
      // Repair all items and check if any were repaired
      boolean anyRepaired = false;
      for (ItemStack item : p.getInventory().getContents()) {
        // Not repairable
        if (item == null || item.getType() == Material.AIR)
          continue;

        // Set the repaired flag uni-directionally
        if (repairItem(item))
          anyRepaired = true;
      }

      // Inform about the process' result
      p.sendMessage(
        cfg.get(anyRepaired ? ConfigKey.REPAIR_INV_SUCCESS : ConfigKey.REPAIR_INV_NONE)
          .withPrefix()
          .asScalar()
      );

      return;
    }

    // Has to have something in their hand
    ItemStack hand = p.getInventory().getItemInMainHand();
    if (hand.getType() == Material.AIR) {
      p.sendMessage(
        cfg.get(ConfigKey.REPAIR_HAND_EMPTY)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    cooldownGuard(
      p, cooldownHandler,
      new ICooldownable() {
        @Override
        public String generateToken() {
          return "cmd_repair";
        }

        @Override
        public int getDurationSeconds() {
          return PlayerPermission.COMMAND_REPAIR_COOLDOWN.getSuffixNumber(p, false).orElse(CD_REPAIR);
        }
      },
      PlayerPermission.COMMAND_REPAIR_COOLDOWN_BYPASS.toString(),
      cfg.get(ConfigKey.ERR_COOLDOWN)
    );

    // Repair and inform about the process' result
    boolean success = repairItem(hand);
    p.sendMessage(
      cfg.get(success ? ConfigKey.REPAIR_HAND_SUCCESS : ConfigKey.REPAIR_HAND_NONE)
        .withPrefix()
        .asScalar()
    );
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Repairs a given item-stack if it's repairable
   * @param i Item to repair
   * @return Whether the item could be repaired
   */
  private boolean repairItem(ItemStack i) {
    ItemMeta meta = i.getItemMeta();

    // Not a damageable item
    if (!(meta instanceof Damageable d))
      return false;

    // Fully repaired already
    if (!d.hasDamage())
      return false;

    // Repair this item
    d.setDamage(0);
    i.setItemMeta(meta);
    return true;
  }
}
