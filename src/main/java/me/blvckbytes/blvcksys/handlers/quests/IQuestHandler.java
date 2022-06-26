package me.blvckbytes.blvcksys.handlers.quests;

import me.blvckbytes.blvcksys.config.sections.QuestSection;
import me.blvckbytes.blvcksys.config.sections.QuestStageSection;
import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

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
   * Get the parent stage of a task
   * @param task Target task
   * @return Parent stage, if exists
   */
  Optional<QuestStageSection> getParentStage(QuestTaskSection task);

  /**
   * Get the parent quest of a stage
   * @param stage Target stage
   * @return Parent quest, if exists
   */
  Optional<QuestSection> getParentQuest(QuestStageSection stage);

  /**
   * Get the token level separator sequence
   */
  String getTokenSeparator();

  /**
   * Fire an existing task due to a given player's action
   * @param p Target player
   * @param token Task token to fire
   */
  void fireTask(Player p, String token);

}
