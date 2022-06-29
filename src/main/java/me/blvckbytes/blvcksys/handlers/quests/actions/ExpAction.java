package me.blvckbytes.blvcksys.handlers.quests.actions;

import me.blvckbytes.blvcksys.config.sections.QuestAction;
import me.blvckbytes.blvcksys.config.sections.QuestExpParameterSecton;
import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/29/2022

  Listens to players gaining experience levels and compares them against exp
  tasks to fire on events which match all requirements.
 */
@AutoConstruct
public class ExpAction extends AQuestAction {

  public ExpAction(
    @AutoInject IQuestHandler questHandler,
    @AutoInject JavaPlugin plugin
  ) {
    super(questHandler, plugin, QuestAction.EXP);
  }

  @EventHandler
  public void onLevel(PlayerExpChangeEvent e) {
    Bukkit.getScheduler().runTask(plugin, () -> experienceChanged(e.getPlayer()));
  }

  /**
   * Called whenever the player's experience level changed
   * @param p Target player
   */
  private void experienceChanged(Player p) {
    for (Map.Entry<String, QuestTaskSection> task : tasks.entrySet()) {

      // Not a valid exp task
      if (!(task.getValue().getParameters() instanceof QuestExpParameterSecton eps))
        continue;

      // Not enough level for this task yet
      if (p.getLevel() < eps.getMaxLevel())
        continue;

      // Fire this task and only stop looping if it was successful
      if (questHandler.fireTask(p, task.getKey()) == TriResult.SUCC)
        break;
    }
  }
}
