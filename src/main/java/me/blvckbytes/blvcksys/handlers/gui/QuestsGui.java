package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.sections.QuestSection;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/26/2022

  View all available quests and thus gain a quick overview of the progress
  made, while a click on the quest redirects to the details page.
*/
@AutoConstruct
public class QuestsGui extends AGui<Object> {

  private final IQuestHandler quests;
  private final QuestStagesGui stagesGui;

  public QuestsGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IQuestHandler quests,
    @AutoInject QuestStagesGui stagesGui
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_QUESTS_TITLE)
        .withVariable("viewer", i.getViewer().getName())
    ), plugin, cfg, textures);

    this.quests = quests;
    this.stagesGui = stagesGui;

    // Refresh the page contents for the viewer that just made progress
    quests.registerProgressInterest(p -> {
      getActiveInstances().getOrDefault(p, new HashSet<>()).forEach(GuiInstance::refreshPageContents);
    });
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();

    inst.addBorder(Material.BLACK_STAINED_GLASS_PANE);
    inst.addPagination(37, 40, 43);

    inst.setPageContents(() -> (
      quests.getQuests().stream().map(quest -> (
        new GuiItem(
          s -> buildQuestItem(p, quest),
          e -> inst.switchTo(AnimationType.SLIDE_LEFT, stagesGui, quest),
          null
        )
      )).toList()
    ));
    
    return true;
  }

  /**
   * Build a representitive item for a quest to be displayed on the main screen
   * @param p Target player to fill in details for
   * @param quest Target quest to display
   * @return Item to be displayed
   */
  private ItemStack buildQuestItem(Player p, QuestSection quest) {
    return (
      quest.getRepresentitive() == null ?
        buildFallbackQuestItem(quest) :
        quest.getRepresentitive()
    )
      .build(buildQuestVariables(p, quest));
  }

  /**
   * Build the most simplest of fallback items possible for the case when the
   * user didn't specify a proper representitive item within the config
   * @param quest Quest to retrieve the display-name from
   * @return Item to be displayed
   */
  private ItemStackBuilder buildFallbackQuestItem(QuestSection quest) {
    return new ItemStackBuilder(Material.BARRIER, 1)
      .withName(quest.getName())
      .withLore(ConfigValue.immediate("§cNo representitive specified"));
  }

  /**
   * Build a map of variables available for the quest display item
   * @param p Target player to fill in details for
   * @param quest Target quest to display
   * @return Variables to apply
   */
  private Map<String, String> buildQuestVariables(Player p, QuestSection quest) {
    Map<String, String> vars = new HashMap<>();
    Optional<Integer> activeStage = quests.getActiveQuestStage(p, quest);
    double progress = quests.getQuestProgress(p, quest);

    // Progress made on the whole quest, including all tasks
    vars.put(
      "progress",
      progress == 0 ? cfg.get(ConfigKey.GUI_QUESTS_NOT_STARTED).asScalar() : (
        progress < 100 ? progress + "%" : cfg.get(ConfigKey.GUI_QUESTS_FINISHED).asScalar()
      )
    );

    // Number of stages available
    vars.put("num_stages", String.valueOf(quest.getStages().length));

    // Currently active stage
    vars.put(
      "curr_stage",
      activeStage.map(integer -> String.valueOf(integer + 1))
        .orElseGet(() -> cfg.get(ConfigKey.GUI_QUESTS_NOT_STARTED).asScalar())
    );

    return vars;
  }
}
