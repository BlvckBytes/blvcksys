package me.blvckbytes.blvcksys.handlers.quests;

import me.blvckbytes.blvcksys.config.sections.QuestSection;
import me.blvckbytes.blvcksys.config.sections.QuestStageSection;
import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.QuestTaskModel;
import org.bukkit.entity.Player;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/26/2022

  Stores all available task data for it's player and checks if tasks are
  still fireable before advancing the state and persisting it.
*/
public class QuestProfile {

  private final Player player;
  private final IPersistence pers;
  private final IQuestHandler qh;

  // Local data cache, as stored in persistence
  private final Map<String, QuestTaskModel> data;

  // Cache for stage completion computation
  private final Set<QuestStageSection> completedStages;

  public QuestProfile(
    Player player,
    IPersistence pers,
    IQuestHandler qh,
    Map<String, QuestTaskModel> data
  ) {
    this.player = player;
    this.pers = pers;
    this.data = data;
    this.qh = qh;
    this.completedStages = new HashSet<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  /**
   * Fire a task which the player just completed and check if it still
   * has counts left. If so, advance the state persistently.
   * @param task Target task
   * @return The new persistent task data model on delta, empty if this task is not fireable for this player
   */
  public Optional<QuestTaskModel> fireTask(QuestTaskSection task) {
    QuestTaskModel model = data.get(task.getToken());

    // This task is not applicable to this player (anymore)
    if (hasCompletedTask(task) || !canReach(task))
      return Optional.empty();

    // Has never fired this task before, create initial model
    if (model == null) {
      model = new QuestTaskModel(player, task.getToken(), 1);
      data.put(task.getToken(), model);
    }

    // Has been fired before, model exists
    else
      model.setCount(model.getCount() + 1);

    pers.store(model);
    return Optional.of(model);
  }

  /**
   * Get the zero-based index of the currently active stage
   * within a quest's list of stages
   * @param quest Target quest
   * @return Index, empty if the player has no progress on this quest yet
   */
  public Optional<Integer> getActiveQuestStage(QuestSection quest) {
    // Has no data for this quest, thus it's not started yet
    if (data.keySet().stream().noneMatch(tk -> tk.startsWith(quest.getToken() + qh.getTokenSeparator())))
      return Optional.empty();

    // Find the first incomplete stage
    for (int i = 0; i < quest.getStages().length; i++) {
      // Found an incomplete stage or at the last possible stage
      if (!isStageComplete(quest.getStages()[i]) || i == quest.getStages().length - 1)
        return Optional.of(i);
    }

    return Optional.empty();
  }

  /**
   * Get the total quest progress in percent, based on how far
   * each of the tasks has been completed
   * @param quest Target quest
   * @return Percentage value rounded to two decimals between 0 and 100
   */
  public double getQuestProgress(QuestSection quest) {
    // Sum all available task counts as well as all completed counts
    int totalCount = 0, completedCount = 0;
    for (QuestStageSection stage : quest.getStages()) {
      for (QuestTaskSection task : stage.getTasks()) {
        totalCount += task.getCount();

        // Add to the completed count, if progress is available
        QuestTaskModel model = data.get(task.getToken());
        if (model != null)
          completedCount += Math.min(task.getCount(), model.getCount());
      }
    }

    return Math.round((float) completedCount / (float) totalCount * 100F * 100F) / 100F;
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Checks whether the player can reach a certain task, which means that the
   * task's parent stage is reachable (all previous stages have been completed)
   * and that either the task order doesn't matter, or all tasks before that
   * task have been completed as well.
   * @param task Target task
   * @return True if the player can reach this task, false if there's still progress missing
   */
  private boolean canReach(QuestTaskSection task) {
    // Could not resolve parent stage
    QuestStageSection stage = qh.getParentStage(task).orElse(null);
    if (stage == null)
      return false;

    // Could not resolve parent quest
    QuestSection quest = qh.getParentQuest(stage).orElse(null);
    if (quest == null)
      return false;

    // Find the last quest that the player has progress on
    QuestStageSection last = null;
    for (QuestStageSection qs : quest.getStages()) {
      // The player has no progress on the current stage, stop looping
      if (data.keySet().stream().noneMatch(dToken -> dToken.startsWith(qs.getToken() + qh.getTokenSeparator()))) {

        // Has no progress at all yet, so the last stage is the first one
        if (last == null)
          last = qs;

        break;
      }

      last = qs;
    }

    // Could not locate the last stage
    if (last == null)
      return false;

    // The last stage is not yet complete and the task in question
    // is not within that stage, so it's either in the past or unreachable
    if (!isStageComplete(last) && Arrays.stream(last.getTasks()).noneMatch(lTask -> lTask.equals(task)))
      return false;

    // Need to check whether all previous tasks have been completed
    if (stage.isTasksInOrder()) {
      for (int i = 0; i < stage.getTasks().length; i++) {
        QuestTaskSection qt = stage.getTasks()[i];

        // Target task reached
        if (qt.equals(task))
          break;

        // Has not yet completed a previous task
        if (!hasCompletedTask(qt))
          return false;
      }
    }

    // All checks passed, this task is reachable
    return true;
  }

  /**
   * Checks whether the player has completed a full stage of tasks
   * @param stage Target stage
   * @return True on completion, false if there's still progress to be made
   */
  private boolean isStageComplete(QuestStageSection stage) {
    if (completedStages.contains(stage))
      return true;

    // The stage is complete if all of it's tasks are complete
    boolean complete = Arrays.stream(stage.getTasks()).allMatch(this::hasCompletedTask);

    // Store answer in cache to save on computation
    if (complete)
      completedStages.add(stage);

    return complete;
  }

  /**
   * Checks whether the player has completed a certain task
   * @param task Target task
   * @return True on completion, false if there's still progress to be made
   */
  private boolean hasCompletedTask(QuestTaskSection task) {
    QuestTaskModel model = data.get(task.getToken());

    // Not even started this task
    if (model == null)
      return false;

    return model.getCount() >= task.getCount();
  }
}
