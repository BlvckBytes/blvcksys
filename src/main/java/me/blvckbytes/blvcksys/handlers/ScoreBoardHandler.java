package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.packets.communicators.IScoreBoardCommunicator;
import me.blvckbytes.blvcksys.packets.communicators.ObjectiveMode;
import me.blvckbytes.blvcksys.packets.communicators.ObjectivePosition;
import me.blvckbytes.blvcksys.packets.communicators.ObjectiveUnit;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Add hearts and xp below the name

@AutoConstruct
public class ScoreBoardHandler implements Listener, IAutoConstructed {

  // Name of the objectives
  private static final String NAME_SIDEBAR = "side";
  private static final String NAME_BELOW_NAME = "below_name";

  private final JavaPlugin plugin;
  private final IConfig cfg;
  private final IScoreBoardCommunicator sbComm;

  // Previous sidebar lines, used for diffing and thus obsolete score deletion
  private Map<Player, List<String>> prevSidebarLines;

  public ScoreBoardHandler(
    @AutoInject JavaPlugin plugin,
    @AutoInject IConfig cfg,
    @AutoInject IScoreBoardCommunicator sbComm
  ) {
    this.prevSidebarLines = new HashMap<>();
    this.plugin = plugin;
    this.cfg = cfg;
    this.sbComm = sbComm;
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    createObjectives(e.getPlayer());

    // Update all online player's sidebar, except the event causing player
    for (Player t : Bukkit.getOnlinePlayers()) {
      if (t != e.getPlayer())
        updateSidebar(t);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Delay (to make sure the player's gone)
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      // Update all online player's sidebar
      for (Player t : Bukkit.getOnlinePlayers())
        updateSidebar(t);
    }, 10);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void initialize() {
    for (Player t : Bukkit.getOnlinePlayers())
      createObjectives(t);
  }

  @Override
  public void cleanup() {
    for (Player t : Bukkit.getOnlinePlayers())
      clearObjectives(t);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Clear all previously created objectives for a player
   * @param t Target player
   */
  private void clearObjectives(Player t) {
    sbComm.sendObjective(t, NAME_SIDEBAR, ObjectiveMode.REMOVE, null, null);
    sbComm.sendObjective(t, NAME_BELOW_NAME, ObjectiveMode.REMOVE, null, null);
  }

  /**
   * Create all objectives for further use for a player
   * @param t Target player
   */
  private void createObjectives(Player t) {
    // Create the sidebar objective
    sbComm.sendObjective(
      t, NAME_SIDEBAR,
      ObjectiveMode.CREATE,
      cfg.get(ConfigKey.SIDEBAR_TITLE).asScalar(),
      ObjectiveUnit.INTEGER
    );

    // Display the sidebar objective
    sbComm.displayObjective(t, NAME_SIDEBAR, ObjectivePosition.SIDEBAR);

    // Create the below name objective
    sbComm.sendObjective(t, NAME_BELOW_NAME, ObjectiveMode.CREATE, null, ObjectiveUnit.INTEGER);

    // Display the below name objective
    sbComm.displayObjective(t, NAME_BELOW_NAME, ObjectivePosition.BELOW_NAME);

    // Display the initial sidebar values
    updateSidebar(t);
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

  /**
   * Update the sidebar for a player
   * @param t Target player
   */
  public void updateSidebar(Player t) {
    // Get the templated list of scoreboard lines
    List<String> scores = cfg.get(ConfigKey.SIDEBAR_LINES)
      .withVariable("num_online", Bukkit.getOnlinePlayers().size())
      .withVariable("name", t.getName())
      .withVariable("num_slots", plugin.getServer().getMaxPlayers())
      .asList();

    // Uniquify all lines to avoid collisions (multiple lines collapsing into a single score)
    scores = uniquifyLines(scores);

    // Remove all previous lines that are not in the current score list
    for (String prev : prevSidebarLines.getOrDefault(t, new ArrayList<>())) {
      if (!scores.contains(prev))
        sbComm.updateScore(t, NAME_SIDEBAR, prev, true, null);
    }

    // Set previous lines cache
    prevSidebarLines.put(t, scores);

    // Update all scores and set their inverse score (since the board is ordered descending)
    for (int i = 0; i < scores.size(); i++)
      sbComm.updateScore(t, NAME_SIDEBAR, scores.get(i), false, scores.size() - 1 - i);
  }
}
