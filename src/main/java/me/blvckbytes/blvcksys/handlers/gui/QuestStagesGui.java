package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.sections.QuestSection;
import me.blvckbytes.blvcksys.config.sections.QuestStageSection;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
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

  View all available stages of a quest and the current progress of their tasks.
*/
@AutoConstruct
public class QuestStagesGui extends AGui<QuestSection> {

  private final IQuestHandler quests;

  @AutoInjectLate
  private QuestsGui questsGui;

  public QuestStagesGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IQuestHandler quests
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_QUEST_STAGES_TITLE)
        .withVariable("quest", i.getArg().getName().asScalar())
    ), plugin, cfg, textures);

    this.quests = quests;

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

    inst.addBorder(Material.BLACK_STAINED_GLASS_PANE);
    inst.addPagination(38, 40, 42);
    inst.addBack(36, questsGui, null, AnimationType.SLIDE_RIGHT);

    inst.setPageContents(() -> (
      Arrays.stream(inst.getArg().getStages()).map(stage -> (
        new GuiItem(
          s -> buildStageItem(p, stage),
          null, null
        )
      )).toList()
    ));
    
    return true;
  }

  /**
   * Build a representitive item for a stage to be displayed
   * @param p Target player to fill in details for
   * @param stage Target stage to display
   * @return Item to be displayed
   */
  private ItemStack buildStageItem(Player p, QuestStageSection stage) {
    return (
      stage.getRepresentitive() == null ?
        buildFallbackStageItem(stage) :
        stage.getRepresentitive()
    )
      .build(buildQuestVariables(p, stage));
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
   * @return Variables to apply
   */
  private Map<String, String> buildQuestVariables(Player p, QuestStageSection stage) {
    Map<String, String> vars = new HashMap<>();

    // TODO: Implement

    return vars;
  }
}
