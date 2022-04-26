package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoConstruct
public class ScoreBoardHandler implements Listener, IAutoConstructed {

  // Name of the sidebar objective
  private static final String NAME_SIDEBAR = "side";

  private final JavaPlugin plugin;
  private final IConfig cfg;

  // Scoreboard handle, scoreboard manager
  private Scoreboard board;
  private final ScoreboardManager man;

  // Sidebar objective
  private Objective objSide;

  // Updating repeating task handle
  private int taskHandle;

  public ScoreBoardHandler(
    @AutoInject JavaPlugin plugin,
    @AutoInject IConfig cfg
  ) {
    this.plugin = plugin;
    this.cfg = cfg;

    // This value is critical, throw on null
    this.man = Bukkit.getScoreboardManager();
    if (this.man == null)
      throw new RuntimeException("Could not get a reference of ScoreboardManager");

    // Work off of the vanilla scoreboard
    this.board = this.man.getMainScoreboard();

    // Create objectives
    createSidebar();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    e.getPlayer().setScoreboard(this.board);
  }

  @Override
  public void cleanup() {
    // Clear the sidebar slot
    board.clearSlot(DisplaySlot.SIDEBAR);
    objSide.unregister();

    // Cancel the repeating task
    Bukkit.getScheduler().cancelTask(taskHandle);
  }

  @Override
  public void initialize() {
    // Update the scores every second to keep variables up to date
    taskHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
      // Get the templated list of scoreboard lines
      List<String> scores = cfg.get(ConfigKey.SIDEBAR_LINES)
        .withVariable("num_online", Bukkit.getOnlinePlayers().size())
        .withVariable("num_slots", plugin.getServer().getMaxPlayers())
        .asList();

      // Uniquify all lines to avoid collisions (multiple lines collapsing into a single score)
      scores = uniquifyLines(scores);

      // Update all scores and set their inverse score (since the board is ordered descending)
      for (int i = 0; i < scores.size(); i++)
        this.objSide.getScore(scores.get(i)).setScore(scores.size() - 1 - i);
    }, 0L, 20L);

    // Send out the scoreboard to all players
    for (Player t : Bukkit.getOnlinePlayers())
      t.setScoreboard(this.board);
  }

  /**
   * Uniquify a list of lines by adding a incrementing number of spaces
   * to each duplicate line
   * @param lines Lines to uniquify
   * @return Uniquified list
   */
  private List<String> uniquifyLines(List<String> lines) {
    List<String> res = new ArrayList<>();
    Map<String, Integer> appendedSpaces = new HashMap<>();

    for (String line : lines) {
      // Not yet known, add and continue
      if (!appendedSpaces.containsKey(line)) {
        appendedSpaces.put(line, 0);
        res.add(line);
        continue;
      }

      // Line is already known, append a unique number of spaces and
      // increase the value in the local map
      int newSpaces = appendedSpaces.get(line) + 1;
      appendedSpaces.put(line, newSpaces);
      res.add(line + " ".repeat(newSpaces));
    }

    return res;
  }

  private void createSidebar() {
    // Unregister a possibly existing board
    this.objSide = this.board.getObjective(NAME_SIDEBAR);
    if (this.objSide != null)
      this.objSide.unregister();

    // Create a new objective, residing in the sidebar, with a title
    this.objSide = this.board.registerNewObjective(NAME_SIDEBAR, "none", NAME_SIDEBAR);
    this.objSide.setDisplaySlot(DisplaySlot.SIDEBAR);
    this.objSide.setDisplayName(cfg.get(ConfigKey.SIDEBAR_TITLE).asScalar());
  }
}
