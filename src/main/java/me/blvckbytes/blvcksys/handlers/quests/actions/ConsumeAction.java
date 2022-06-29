package me.blvckbytes.blvcksys.handlers.quests.actions;

import me.blvckbytes.blvcksys.config.sections.ItemStackSection;
import me.blvckbytes.blvcksys.config.sections.QuestItemParameterSection;
import me.blvckbytes.blvcksys.config.sections.QuestAction;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/27/2022

  Listens to players consuming items and compares them against consuming
  tasks to fire on items which match all requirements.
 */
@AutoConstruct
public class ConsumeAction extends AQuestAction<QuestItemParameterSection> {

  public ConsumeAction(
    @AutoInject IQuestHandler questHandler,
    @AutoInject JavaPlugin plugin
  ) {
    super(questHandler, plugin, QuestAction.CONSUME);
  }

  @EventHandler
  public void onConsume(PlayerItemConsumeEvent e) {
    for (Map.Entry<String, QuestItemParameterSection> task : tasks.entrySet()) {
      // Not matching this task's parameter requirements
      if (!compareItems(e.getItem(), task.getValue()))
        continue;

      // Fire this task and only stop looping if it was successful
      if (questHandler.fireTask(e.getPlayer(), task.getKey()) == TriResult.SUCC)
        break;
    }
  }

  /**
   * Compares the provided item against all specified items within the parameter
   * and checks if it matches all properties of any entry.
   * @param item Item to compare
   * @param isp Parameter to compare against
   * @return True if any entry matches the item, false otherwise
   */
  private boolean compareItems(ItemStack item, QuestItemParameterSection isp) {
    // Consumed items will always be consumed one at a time
    item = item.clone();
    item.setAmount(1);

    for (ItemStackSection paramItem : isp.getItems()) {
      // Check if this section describes the item in question
      if (paramItem.describesItem(item))
        return true;
    }

    // No matches found
    return false;
  }
}
