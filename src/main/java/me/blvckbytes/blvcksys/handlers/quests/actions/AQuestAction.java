package me.blvckbytes.blvcksys.handlers.quests.actions;

import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.sections.QuestAction;
import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/26/2022

  Implements basic functionality of all quest action listeners
 */
public abstract class AQuestAction<T extends AConfigSection> implements Listener, IAutoConstructed {

  protected final IQuestHandler questHandler;
  protected final JavaPlugin plugin;
  protected final Map<String, T> tasks;

  @SuppressWarnings("unchecked")
  protected AQuestAction(IQuestHandler questHandler, JavaPlugin plugin, QuestAction action) {
    this.questHandler = questHandler;
    this.plugin = plugin;

    // Only cache tasks which match this handler's action to reduce time complexity
    this.tasks = new LinkedHashMap<>();
    for (Map.Entry<String, QuestTaskSection> taskE : questHandler.getTasks().entrySet()) {
      QuestTaskSection task = taskE.getValue();
      if (
        // Has the action handled by this class
        task.getAction() == action &&
        // Has parsed the proper runtime parameter
        action.getParameterType().isInstance(task.getParameters())
      ) {
        // Cast the parameter now, as it's safe to assume it matches in type
        this.tasks.put(
          taskE.getKey(),
          (T) action.getParameterType().cast(task.getParameters())
        );
      }
    }
  }

  @Override
  public void initialize() {}

  @Override
  public void cleanup() {}
}
