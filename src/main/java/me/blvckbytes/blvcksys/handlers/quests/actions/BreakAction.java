package me.blvckbytes.blvcksys.handlers.quests.actions;

import me.blvckbytes.blvcksys.config.sections.*;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Listens to players breaking blocks and compares them against breaking
  tasks to fire on items which match all requirements.
 */
@AutoConstruct
public class BreakAction extends AQuestAction {

  public BreakAction(
    @AutoInject IQuestHandler questHandler,
    @AutoInject JavaPlugin plugin
  ) {
    super(questHandler, plugin, QuestAction.BREAK);
  }

  @EventHandler
  public void onBreak(BlockBreakEvent e) {
    for (Map.Entry<String, QuestTaskSection> task : tasks.entrySet()) {

      // Not a valid break task
      if (!(task.getValue().getParameters() instanceof QuestBreakParameterSecton bps))
        continue;

      // Not matching this task's parameter requirements
      if (!compareBreak(e.getPlayer(), e.getBlock(), bps))
        continue;

      // Fire this task and only stop looping if it was successful
      if (questHandler.fireTask(e.getPlayer(), task.getKey()) == TriResult.SUCC)
        break;
    }
  }

  /**
   * Compares the provided block against all specified items within the parameter
   * and checks if it matches all properties of any entry.
   * @param block Block to compare
   * @param bps Parameter to compare against
   * @return True if any entry matches the block, false otherwise
   */
  private boolean compareBreak(Player p, Block block, QuestBreakParameterSecton bps) {

    // Validate the item held in the main hand
    ItemStack hand = p.getInventory().getItemInMainHand();
    if (bps.getHand() != null && !bps.getHand().describesItem(hand))
      return false;

    // Validate that the block dropped at least what's required
    if (bps.getYield().length > 0) {
      Collection<ItemStack> drops = block.getDrops(hand, p);

      for (ItemStackSection dropSect : bps.getYield()) {
        // Current drop section didn't describe any of the available drops
        if (drops.stream().noneMatch(dropSect::describesItem))
          return false;
      }
    }

    // Validate that the broken block itself matches
    if (bps.getBlock() != null && !bps.getBlock().describesBlock(block))
      return false;

    // All checks passed
    return true;
  }
}
