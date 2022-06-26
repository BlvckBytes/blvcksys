package me.blvckbytes.blvcksys.handlers.quests.actions;

import me.blvckbytes.blvcksys.config.sections.QuestAction;
import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/26/2022

  Implements basic functionality of all quest action listeners
 */
public abstract class AQuestAction implements Listener, IAutoConstructed {

  protected final IQuestHandler questHandler;
  protected final JavaPlugin plugin;
  protected final Map<String, QuestTaskSection> tasks;

  protected AQuestAction(IQuestHandler questHandler, JavaPlugin plugin, QuestAction action) {
    this.questHandler = questHandler;
    this.plugin = plugin;
    this.tasks = new HashMap<>();

    // Only cache tasks which match this handler's action to reduce time complexity
    for (Map.Entry<String, QuestTaskSection> taskE : questHandler.getTasks().entrySet()) {
      if (taskE.getValue().getAction() == action)
        this.tasks.put(taskE.getKey(), taskE.getValue());
    }
  }

  @Override
  public void initialize() {}

  @Override
  public void cleanup() {}
}
