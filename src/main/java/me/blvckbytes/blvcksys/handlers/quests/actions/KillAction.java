package me.blvckbytes.blvcksys.handlers.quests.actions;

import me.blvckbytes.blvcksys.config.sections.*;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/29/2022

  Listens to players killing other entities and compares them against killing
  tasks to fire on entities which match all requirements.
 */
@AutoConstruct
public class KillAction extends AQuestAction {

  public KillAction(
    @AutoInject IQuestHandler questHandler,
    @AutoInject JavaPlugin plugin
  ) {
    super(questHandler, plugin, QuestAction.KILL);
  }

  @EventHandler
  public void onDamage(EntityDamageByEntityEvent e) {
    // Wasn't caused by a player
    if (!(e.getDamager() instanceof Player p))
      return;

    // Wasn't done to a living entity
    if (!(e.getEntity() instanceof LivingEntity victim))
      return;

    // Didn't kill the entity
    if (victim.getHealth() > e.getDamage())
      return;

    playerHasKilled(p, victim);
  }

  /**
   * Called whenever a player has killed another entity
   * @param p Target player
   * @param victim Killed entity
   */
  private void playerHasKilled(Player p, LivingEntity victim) {
    for (Map.Entry<String, QuestTaskSection> task : tasks.entrySet()) {

      // Not a valid kill task
      if (!(task.getValue().getParameters() instanceof QuestKillParameterSecton kps))
        continue;

      // Not matching this task's parameter requirements
      if (!compareKill(p, victim, kps))
        continue;

      // Fire this task and only stop looping if it was successful
      if (questHandler.fireTask(p, task.getKey()) == TriResult.SUCC)
        break;
    }
  }

  /**
   * Compares the provided entity against all specified entities within the parameter
   * and checks if it matches all properties of any entry, also checks the hand item.
   * @param p Target player
   * @param victim Entity to compare
   * @param kps Parameter to compare against
   * @return True if any entry matches the kill, false otherwise
   */
  private boolean compareKill(Player p, LivingEntity victim, QuestKillParameterSecton kps) {
    // Validate the item held in the main hand
    ItemStack hand = p.getInventory().getItemInMainHand();
    if (kps.getHand() != null && !kps.getHand().describesItem(hand))
      return false;

    // Target entity not described by this parameter
    if (Arrays.stream(kps.getEntities()).noneMatch(les -> les.describesEntity(victim)))
      return false;

    // Validate that the victim was at the right location
    if (kps.getLocations().length > 0 && Arrays.stream(kps.getLocations()).noneMatch(l -> l.describesLocation(victim.getLocation())))
      return false;

    // All checks passed
    return true;
  }
}
