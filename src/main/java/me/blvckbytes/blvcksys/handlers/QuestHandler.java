package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.sections.QuestSection;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/25/2022

  Loads quest description files into memory, listens to all available quest
  actions and handles advancing and storing player states.
*/
@AutoConstruct
public class QuestHandler implements IQuestHandler, IAutoConstructed {

  // Name of the folder within the plugin's data folder which
  // contains all quest description files to be loaded
  private static final String foldername = "quests";

  private final List<QuestSection> quests;
  private final JavaPlugin plugin;
  private final IConfig cfg;

  public QuestHandler(
    @AutoInject JavaPlugin plugin,
    @AutoInject IConfig cfg
  ) {
    this.plugin = plugin;
    this.cfg = cfg;
    this.quests = new ArrayList<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void cleanup() {}

  @Override
  public void initialize() {
    this.loadQuests();
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Load all available quest files from the containing folder into memory models
   */
  private void loadQuests() {
    File container = new File(plugin.getDataFolder(), foldername);
    File[] files = container.listFiles();

    if (files == null)
      return;

    // Loop all files in the containing folder in the data folder
    for (File file : files) {

      // Not a yaml file
      if (!file.getName().endsWith(".yml"))
        continue;

      // Try to load the config
      cfg.reader(foldername + "/" + file.getName())
        // Parse the top level (full file) into a quest
        .flatMap(cr -> cr.parseValue(null, QuestSection.class))
        // Store in local cache
        .ifPresent(quests::add);
    }
  }
}
