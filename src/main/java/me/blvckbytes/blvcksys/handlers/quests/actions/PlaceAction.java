package me.blvckbytes.blvcksys.handlers.quests.actions;

import me.blvckbytes.blvcksys.config.sections.QuestAction;
import me.blvckbytes.blvcksys.config.sections.QuestPlaceParameterSecton;
import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/29/2022

  Listens to players placing blocks and compares them against placing
  tasks to fire on items which match all requirements.
 */
@AutoConstruct
public class PlaceAction extends AQuestAction {

  public PlaceAction(
    @AutoInject IQuestHandler questHandler,
    @AutoInject JavaPlugin plugin
  ) {
    super(questHandler, plugin, QuestAction.PLACE);
  }

  @EventHandler
  public void onPlace(BlockPlaceEvent e) {
    for (Map.Entry<String, QuestTaskSection> task : tasks.entrySet()) {

      // Not a valid place task
      if (!(task.getValue().getParameters() instanceof QuestPlaceParameterSecton pps))
        continue;

      // Not matching this task's parameter requirements
      if (!comparePlace(e.getPlayer(), e.getBlockPlaced(), pps))
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
   * @param pps Parameter to compare against
   * @return True if any entry matches the block, false otherwise
   */
  private boolean comparePlace(Player p, Block block, QuestPlaceParameterSecton pps) {

    // Validate the item held in the main hand
    ItemStack hand = p.getInventory().getItemInMainHand();
    if (pps.getHand() != null && !pps.getHand().describesItem(hand))
      return false;

    // Validate that the placed block itself matches
    if (pps.getBlock() != null && !pps.getBlock().describesBlock(block))
      return false;

    // All checks passed
    return true;
  }
}
