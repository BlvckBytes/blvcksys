package me.blvckbytes.blvcksys.handlers.quests;

import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.QuestTaskModel;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/26/2022

  Stores all available task data for it's player and checks if tasks are
  still fireable before advancing the state and persisting it.
*/
public class QuestProfile {

  private final Player player;
  private final IPersistence pers;
  private final Map<String, QuestTaskModel> data;

  public QuestProfile(
    Player player, IPersistence pers,
    Map<String, QuestTaskModel> data
  ) {
    this.player = player;
    this.pers = pers;
    this.data = data;
  }

  /**
   * Fire a task which the player just completed and check if it still
   * has counts left. If so, advance the state persistently.
   * @param token Target token used for persistence
   * @param task Target task
   * @return The new persistent task data model on delta, empty if this task is not fireable for this player
   */
  public Optional<QuestTaskModel> fireTask(String token, QuestTaskSection task) {
    QuestTaskModel model = data.get(token);
    boolean delta = false;

    // Has never fired this task before, create initial model
    if (model == null) {
      model = new QuestTaskModel(player, token, 1);
      data.put(token, model);
      delta = true;
    }

    // Has been fired before, model exists
    else {
      // If this task can be fired again, increase count
      if (model.getCount() < task.getCount()) {
        model.setCount(model.getCount() + 1);
        delta = true;
      }
    }

    if (delta) {
      pers.store(model);
      return Optional.of(model);
    }

    return Optional.empty();
  }
}
