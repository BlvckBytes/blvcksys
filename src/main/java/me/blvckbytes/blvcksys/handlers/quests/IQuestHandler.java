package me.blvckbytes.blvcksys.handlers.quests;

import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import org.bukkit.entity.Player;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/25/2022

  Public interfaces which the quest handler provides to other consumers.
*/
public interface IQuestHandler {

  /**
   * Get all existing tasks loaded from configs
   */
  Map<String, QuestTaskSection> getTasks();

  /**
   * Fire an existing task due to a given player's action
   * @param p Target player
   * @param token Task token to fire
   */
  void fireTask(Player p, String token);

}
