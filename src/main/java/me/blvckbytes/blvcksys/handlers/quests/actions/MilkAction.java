package me.blvckbytes.blvcksys.handlers.quests.actions;

import me.blvckbytes.blvcksys.config.sections.QuestAction;
import me.blvckbytes.blvcksys.config.sections.QuestMilkParameterSecton;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/30/2022

  Listens to players milking cows and compares them against milk
  tasks to fire on events which match all requirements.
 */
@AutoConstruct
public class MilkAction extends AQuestAction<QuestMilkParameterSecton> {

  public MilkAction(
    @AutoInject IQuestHandler questHandler,
    @AutoInject JavaPlugin plugin
  ) {
    super(questHandler, plugin, QuestAction.MILK);
  }

  @EventHandler
  public void onInteract(PlayerInteractEntityEvent e) {
    if (
      // Interacted with a sheep
      e.getRightClicked() instanceof Cow cow &&
      // Held an empty bucket in their main hand
      e.getPlayer().getInventory().getItemInMainHand().getType() == Material.BUCKET
    )
      playerMilked(e.getPlayer(), cow);
  }

  /**
   * Called whenever the player milks a cow
   * @param p Target player
   * @param cow Cow that has been milked
   */
  private void playerMilked(Player p, Cow cow) {
    for (Map.Entry<String, QuestMilkParameterSecton> task : tasks.entrySet()) {
      QuestMilkParameterSecton sps = task.getValue();

      // Cow is at the wrong location
      if (sps.getLocations().length > 0 && Arrays.stream(sps.getLocations()).noneMatch(loc -> loc.describesLocation(cow.getLocation())))
        continue;

      // Fire this task and only stop looping if it was successful
      if (questHandler.fireTask(p, task.getKey()) == TriResult.SUCC)
        break;
    }
  }
}
