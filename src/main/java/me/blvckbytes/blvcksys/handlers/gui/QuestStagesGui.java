package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.sections.QuestSection;
import me.blvckbytes.blvcksys.config.sections.QuestStageSection;
import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import net.minecraft.util.Tuple;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/26/2022

  View all available stages of a quest and the current progress of their tasks.
*/
@AutoConstruct
public class QuestStagesGui extends AGui<QuestSection> {

  private final IQuestHandler quests;
  private final IStdGuiItemProvider stdGuiItemProvider;

  @AutoInjectLate
  private QuestsGui questsGui;

  public QuestStagesGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IQuestHandler quests,
    @AutoInject IStdGuiItemProvider stdGuiItemProvider
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_QUEST_STAGES_TITLE)
        .withVariable("quest", i.getArg().getName().asScalar())
    ), plugin, cfg, textures);

    this.quests = quests;
    this.stdGuiItemProvider = stdGuiItemProvider;

    // Refresh the page contents for the viewer that just made progress
    quests.registerProgressInterest(p -> {
      getActiveInstances().getOrDefault(p, new HashSet<>()).forEach(GuiInstance::refreshPageContents);
    });
  }

  @Override
  protected boolean closed(GuiInstance<QuestSection> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<QuestSection> inst) {
    Player p = inst.getViewer();

    inst.addBorder(stdGuiItemProvider);

    // Paginator
    inst.addPagination("38", "40", "42", stdGuiItemProvider);

    inst.addBack("36", stdGuiItemProvider, questsGui, null, AnimationType.SLIDE_RIGHT);

    inst.setPageContents(() -> {
      List<GuiItem> contents = new ArrayList<>();

      for (int i = 0; i < inst.getArg().getStages().length; i++) {
        QuestStageSection stage = inst.getArg().getStages()[i];

        int fI = i;
        contents.add(new GuiItem(
          s -> buildStageItem(
            p, stage,
            fI == 0 || quests.isStageComplete(p, inst.getArg().getStages()[fI - 1])
          ),
          null, null
        ));
      }

      return contents;
    });
    
    return true;
  }

  /**
   * Build a representitive item for a stage to be displayed
   * @param p Target player to fill in details for
   * @param stage Target stage to display
   * @param reachable Whether this stage is reachable
   * @return Item to be displayed
   */
  private ItemStack buildStageItem(Player p, QuestStageSection stage, boolean reachable) {
    Tuple<Map<String, String>, Boolean> variables = buildQuestVariables(p, stage);

    return (
      stage.getRepresentitive() == null ?
        buildFallbackStageItem(stage) :
        stage.getRepresentitive().copy()
    )
      // Only display locked messages when the quest is not already completed anyways
      .withLore(cfg.get(ConfigKey.GUI_QUEST_STAGES_LOCKED), !reachable && !variables.b())
      .build(variables.a());
  }

  /**
   * Build the most simplest of fallback items possible for the case when the
   * user didn't specify a proper representitive item within the config
   * @param stage Stage to retrieve the display-name from
   * @return Item to be displayed
   */
  private ItemStackBuilder buildFallbackStageItem(QuestStageSection stage) {
    return new ItemStackBuilder(Material.BARRIER, 1)
      .withName(stage.getName())
      .withLore(ConfigValue.immediate("§cNo representitive specified"));
  }

  /**
   * Build a map of variables available for the stage display item
   * @param p Target player to fill in details for
   * @param stage Target stage to display
   * @return Variables to apply and if that stage is complete
   */
  private Tuple<Map<String, String>, Boolean> buildQuestVariables(Player p, QuestStageSection stage) {
    ConfigValue cv = ConfigValue.makeEmpty();
    int tasksTotal = 0, tasksCompleted = 0;

    // Add custom variables for each task, identified by it's sequence ID
    for (int i = 0; i < stage.getTasks().length; i++) {
      QuestTaskSection task = stage.getTasks()[i];
      int completed = quests.getTaskCompletedCount(p, task);

      tasksCompleted += completed;
      tasksTotal += task.getCount();

      cv.withVariable("task_" + i + "_completed", String.valueOf(completed));
      cv.withVariable("task_" + i + "_total", String.valueOf(task.getCount()));
    }

    double progress = (tasksCompleted * 100D) / tasksTotal;

    // Progress made on the whole quest, including all tasks
    cv.withVariable("progress", progress, "%");

    // Progress number as a bar
    cv.withVariable("progress_bar", questsGui.buildProgressBar(progress));

    return new Tuple<>(cv.exportVariables(), progress >= 100);
  }
}
