package me.blvckbytes.blvcksys.handlers.quests;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.sections.QuestSection;
import me.blvckbytes.blvcksys.config.sections.QuestStageSection;
import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.QuestTaskModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/25/2022

  Loads quest description files into memory, generates tokens and handles firing tasks.
*/
@AutoConstruct
public class QuestHandler implements IQuestHandler, IAutoConstructed, Listener {

  // Name of the folder within the plugin's data folder which
  // contains all quest description files to be loaded
  private static final String FOLDERNAME = "quests";

  // Separator string of unique tokens, injected between section names
  private static final String TOKEN_SEP = "__";

  @Getter
  private final Map<String, QuestTaskSection> tasks;
  private final Map<String, QuestStageSection> stages;
  private final Map<String, QuestSection> quests;
  private final Map<Player, QuestProfile> playerdata;

  private final JavaPlugin plugin;
  private final IConfig cfg;
  private final IPersistence pers;

  public QuestHandler(
    @AutoInject JavaPlugin plugin,
    @AutoInject IConfig cfg,
    @AutoInject IPersistence pers
  ) {
    this.plugin = plugin;
    this.cfg = cfg;
    this.pers = pers;
    this.quests = new HashMap<>();
    this.stages = new HashMap<>();
    this.tasks = new HashMap<>();
    this.playerdata = new HashMap<>();
    this.importQuestsFromConfig();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void fireTask(Player p, String token) {
    // TODO: Validate functionality
    System.out.println("fireTask() " + token);
    QuestProfile profile = playerdata.get(p);

    // Player not online (or not loaded)
    if (profile == null)
      return;

    System.out.println("passed profile");

    QuestTaskSection task = tasks.get(token);

    // Task unknow
    if (task == null)
      return;

    System.out.println("passed task");

    String[] tokenData = token.split(TOKEN_SEP);
    QuestSection quest = quests.get(tokenData[0]);
    QuestStageSection stage = stages.get(tokenData[1]);

    System.out.println(quest == null);
    System.out.println(stage == null);

    // Could not locate parents
    if (quest == null || stage == null)
      return;

    // Relay handling to the profile
    QuestTaskModel model = profile.fireTask(token, task).orElse(null);

    // Something changed, notify the player
    if (model != null) {
      System.out.println("passed model");
      // TODO: Notify the player
      p.sendMessage("Fired task token=" + token + " (" + model.getCount() + "/" + task.getCount() + ")");
    }
  }

  //=========================================================================//
  //                                 Listener                                //
  //=========================================================================//

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    loadPlayerData(e.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    playerdata.remove(e.getPlayer());
  }

  @Override
  public void initialize() {
    for (Player t : Bukkit.getOnlinePlayers())
      loadPlayerData(t);
  }

  @Override
  public void cleanup() {}

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Load all task data of a given player into memory
   * @param p Target player
   */
  private void loadPlayerData(Player p) {
    Map<String, QuestTaskModel> data = pers.find(buildQuery(p)).stream()
      .collect(Collectors.toMap(QuestTaskModel::getToken, task -> task));
    playerdata.put(p, new QuestProfile(p, pers, data));
  }

  /**
   * Load all available quest files from the containing folder into memory models
   */
  private void importQuestsFromConfig() {
    File container = new File(plugin.getDataFolder(), FOLDERNAME);
    File[] files = container.listFiles();

    if (files == null)
      return;

    // Loop all files in the containing folder in the data folder
    for (File file : files) {

      // Not a yaml file
      String fn = file.getName();
      if (!fn.endsWith(".yml"))
        continue;

      // Try to load the config
      cfg.reader(FOLDERNAME + "/" + fn.substring(0, fn.indexOf('.')))
        // Parse the top level (full file) into a quest
        .flatMap(cr -> cr.parseValue(null, QuestSection.class))
        // Store in local cache
        .ifPresent(q -> {
          // Cannot store quests without a name
          if (q.getName() == null)
            return;

          String questName = normalizeName(q.getName());
          quests.put(questName, q);

          // This quest has no stages defined yet
          if (q.getStages() == null)
            return;

          // Loop all stages
          for (QuestStageSection stage : q.getStages()) {

            // Cannot store stages without a name
            if (stage.getName() == null)
              continue;

            stages.put(buildToken(q.getName(), stage.getName(), null), stage);

            // Loop all tasks and pre-compute their full token
            for (int i = 0; i < stage.getTasks().length; i++)
              tasks.put(buildToken(q.getName(), stage.getName(), i), stage.getTasks()[i]);
          }
        });
    }
  }

  /**
   * Normalize the name value of a quest section to be used as an unique identifier
   * @param name Name input
   * @return Normalized name output
   */
  private String normalizeName(ConfigValue name) {
    // Remove color and leading/trailing whitespace, make lower case
    return ChatColor.stripColor(name.asScalar()).trim().toLowerCase()
      // Replace spaces with underscores and strip off all non-printable ascii or non-ascii chars
      .replace(" ", "_").replaceAll("[^\\x20-\\x7E]", "")
      // Separators are not allowed
      .replace(TOKEN_SEP, "");
  }

  /**
   * Builds a unique token for the full path of a task within it's quest
   * @param questName Name of the parent quest
   * @param stageName Name of the parent stage
   * @param taskIndex Index of the task within the stage's tasks (optional)
   */
  private String buildToken(ConfigValue questName, ConfigValue stageName, @Nullable Integer taskIndex) {
    String parent = normalizeName(questName) + TOKEN_SEP + normalizeName(stageName);
    if (taskIndex != null)
      parent += TOKEN_SEP + taskIndex;
    return parent;
  }

  /**
   * Builds the query to select all stored tasks of a player
   * @param target Target player
   */
  private QueryBuilder<QuestTaskModel> buildQuery(OfflinePlayer target) {
    return new QueryBuilder<>(
      QuestTaskModel.class,
      "player__uuid", EqualityOperation.EQ, target.getUniqueId()
    );
  }
}