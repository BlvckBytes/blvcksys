package me.blvckbytes.blvcksys.handlers.quests.actions;

import me.blvckbytes.blvcksys.config.sections.QuestAction;
import me.blvckbytes.blvcksys.config.sections.QuestShearParameterSecton;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/30/2022

  Listens to players shearing sheeps and compares them against shear
  tasks to fire on events which match all requirements.
 */
@AutoConstruct
public class ShearAction extends AQuestAction<QuestShearParameterSecton> {

  public ShearAction(
    @AutoInject IQuestHandler questHandler,
    @AutoInject JavaPlugin plugin
  ) {
    super(questHandler, plugin, QuestAction.SHEAR);
  }

  @EventHandler
  public void onInteract(PlayerInteractEntityEvent e) {
    if (
      // Interacted with a sheep
      e.getRightClicked() instanceof Sheep sheep &&
      // Held a shear in their main hand
      e.getPlayer().getInventory().getItemInMainHand().getType() == Material.SHEARS &&
      // The sheep wasn't sheared already
      !sheep.isSheared()
    )
      playerSheared(e.getPlayer(), sheep);
  }

  /**
   * Called whenever the player shears a sheep
   * @param p Target player
   * @param sheep Sheep that has been sheared
   */
  private void playerSheared(Player p, Sheep sheep) {
    for (Map.Entry<String, QuestShearParameterSecton> task : tasks.entrySet()) {
      QuestShearParameterSecton sps = task.getValue();

      // Sheep is at the wrong location
      if (sps.getLocations().length > 0 && Arrays.stream(sps.getLocations()).noneMatch(loc -> loc.describesLocation(sheep.getLocation())))
        continue;

      // Sheep has the wrong color
      if (sps.getColors().length > 0 && Arrays.stream(sps.getColors()).noneMatch(color -> color.equals(sheep.getColor())))
        continue;

      // Fire this task and only stop looping if it was successful
      if (questHandler.fireTask(p, task.getKey()) == TriResult.SUCC)
        break;
    }
  }
}
