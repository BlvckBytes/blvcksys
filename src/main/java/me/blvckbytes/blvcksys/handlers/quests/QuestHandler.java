package me.blvckbytes.blvcksys.handlers.quests;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.ConfigKey;
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
import me.blvckbytes.blvcksys.util.logging.ILogger;
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
import java.util.function.Consumer;
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
  private final List<Consumer<Player>> progressInterests;

  private final JavaPlugin plugin;
  private final IConfig cfg;
  private final IPersistence pers;
  private final ILogger logger;

  public QuestHandler(
    @AutoInject JavaPlugin plugin,
    @AutoInject IConfig cfg,
    @AutoInject IPersistence pers,
    @AutoInject ILogger logger
  ) {
    this.plugin = plugin;
    this.cfg = cfg;
    this.pers = pers;
    this.logger = logger;

    this.quests = new HashMap<>();
    this.stages = new HashMap<>();
    this.tasks = new HashMap<>();
    this.playerdata = new HashMap<>();
    this.progressInterests = new ArrayList<>();

    this.importQuestsFromConfig();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void fireTask(Player p, String token) {
    // Player not online (or not loaded)
    QuestProfile profile = playerdata.get(p);
    if (profile == null)
      return;

    // Task unknow
    QuestTaskSection task = tasks.get(token);
    if (task == null)
      return;

    String[] tokenData = token.split(TOKEN_SEP);
    QuestSection quest = quests.get(tokenData[0]);
    QuestStageSection stage = stages.get(tokenData[0] + TOKEN_SEP + tokenData[1]);

    // Could not locate parents
    if (quest == null || stage == null)
      return;

    // Relay handling to the profile
    QuestTaskModel model = profile.fireTask(task).orElse(null);

    // Something changed, notify the player
    if (model != null) {
      // Notify all interest registrations
      progressInterests.forEach(c -> c.accept(p));

      // Find the sequence number of this task within it's parent
      int taskSeq;
      for (taskSeq = 0; taskSeq < stage.getTasks().length; taskSeq++) {
        if (stage.getTasks()[taskSeq].equals(task))
          break;
      }

      p.sendMessage(
        cfg.get(ConfigKey.QUESTS_TASK_FULFILLED)
          .withPrefixes()
          .withVariable("completed_count", model.getCount())
          .withVariable("total_count", task.getCount())
          .withVariable("stage_name", stage.getName().asScalar())
          .withVariable("quest_name", quest.getName().asScalar())
          .withVariable("task_number", taskSeq + 1)
          .asScalar()
      );
    }
  }

  @Override
  public void registerProgressInterest(Consumer<Player> target) {
    this.progressInterests.add(target);
  }

  @Override
  public Optional<Integer> getActiveQuestStage(Player p, QuestSection quest) {
    QuestProfile profile = playerdata.get(p);
    if (profile == null)
      return Optional.empty();
    return profile.getActiveQuestStage(quest);
  }

  @Override
  public double getQuestProgress(Player p, QuestSection quest) {
    QuestProfile profile = playerdata.get(p);
    if (profile == null)
      return 0;
    return profile.getQuestProgress(quest);
  }

  @Override
  public List<QuestSection> getQuests() {
    return new ArrayList<>(quests.values());
  }

  @Override
  public Optional<QuestStageSection> getParentStage(QuestTaskSection task) {
    String[] tData = task.getToken().split(TOKEN_SEP);

    if (tData.length < 2)
      return Optional.empty();

    return stages.entrySet().stream()
      .filter(e -> e.getKey().equals(tData[0] + TOKEN_SEP + tData[1]))
      .map(Map.Entry::getValue)
      .findFirst();
  }

  @Override
  public Optional<QuestSection> getParentQuest(QuestStageSection stage) {
    String[] tData = stage.getToken().split(TOKEN_SEP);

    if (tData.length < 1)
      return Optional.empty();

    return quests.entrySet().stream()
      .filter(e -> e.getKey().equals(tData[0]))
      .map(Map.Entry::getValue)
      .findFirst();
  }

  @Override
  public String getTokenSeparator() {
    return TOKEN_SEP;
  }

  @Override
  public void initialize() {
    for (Player t : Bukkit.getOnlinePlayers())
      loadPlayerData(t);
  }

  @Override
  public void cleanup() {}

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
    playerdata.put(p, new QuestProfile(p, pers, this, data));
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

          q.setToken(normalizeName(q.getName()));
          quests.put(q.getToken(), q);

          // This quest has no stages defined yet
          if (q.getStages() == null)
            return;

          // Loop all stages
          for (QuestStageSection stage : q.getStages()) {

            // Cannot store stages without a name
            if (stage.getName() == null)
              continue;

            stage.setToken(buildToken(q.getName(), stage.getName(), null));
            stages.put(stage.getToken(), stage);

            // Loop all tasks and pre-compute their full token
            for (int i = 0; i < stage.getTasks().length; i++) {
              QuestTaskSection qs = stage.getTasks()[i];
              qs.setToken(buildToken(q.getName(), stage.getName(), i));

              // Duplicate token encountered, skip and notify the console
              if (tasks.containsKey(qs.getToken())) {
                logger.logError("Skipping duplicate token quest task '" + qs.getToken() + "', please choose unique names!");
                continue;
              }

              tasks.put(qs.getToken(), qs);
            }
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
