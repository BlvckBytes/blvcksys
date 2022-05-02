package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.packets.communicators.objective.IObjectiveCommunicator;
import me.blvckbytes.blvcksys.packets.communicators.objective.ObjectiveMode;
import me.blvckbytes.blvcksys.packets.communicators.objective.ObjectivePosition;
import me.blvckbytes.blvcksys.packets.communicators.objective.ObjectiveUnit;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  Handles both the sidebar scoreboard as well as the scoreboard below a player's
  name above their avatar for all players. All created objectives are destroyed
  before the end of this module's lifecycle. Levels and hearts below the name are
  only updated on an actual numeric delta by listening to all corresponding events
  that affect those metrics. The sidebar is also only refreshed on changes.
*/
@AutoConstruct
public class ObjectiveHandler implements Listener, IAutoConstructed, IObjectiveHandler {

  // Name of the sidebar objective
  private static final String NAME_SIDEBAR = "side";

  // Name of the objective below each player's name
  // where the %s indicates the text's owner
  private static final String NAME_BELOW_NAME = "below_%s";

  // Delay in ticks to wait after events to take effect
  private static final long EVENT_DELAY = 10;

  private final JavaPlugin plugin;
  private final IConfig cfg;
  private final IObjectiveCommunicator oComm;

  // Previous sidebar lines, used for diffing and thus obsolete score deletion
  private final Map<Player, List<String>> prevSidebarLines;

  // List of all players that a player has registered objectives for below name for
  private final Map<Player, List<Player>> knownBelowNames;

  // Map of each player to their previous levels, used to check for level delta when leveling up
  private final Map<Player, Integer> prevLevels;

  // Each player has a bitmask of below name flags
  private final Map<Player, Integer> belowNameFlags;

  public ObjectiveHandler(
    @AutoInject JavaPlugin plugin,
    @AutoInject IConfig cfg,
    @AutoInject IObjectiveCommunicator oComm
  ) {
    this.prevSidebarLines = new HashMap<>();
    this.knownBelowNames = new HashMap<>();
    this.prevLevels = new HashMap<>();
    this.belowNameFlags = new HashMap<>();

    this.plugin = plugin;
    this.cfg = cfg;
    this.oComm = oComm;
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    Player p = e.getPlayer();

    // Create the sidebar for the joined player
    createSidebarObjective(p);

    // Update the sidebar for all other players
    for (Player t : Bukkit.getOnlinePlayers()) {
      // Skip self
      if (t == p)
        continue;

      // Update the sidebar for all other players
      updateSidebar(t);

      // Create a below name objective for the joined player on all other clients
      createBelowNameObjective(t, e.getPlayer());

      // Create a below name objective on all other clients for the joined player
      createBelowNameObjective(p, t);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Remove this player form all player's below name
    for (Player t : Bukkit.getOnlinePlayers()) {
      if (t != e.getPlayer())
        clearBelowNameObjective(t, e.getPlayer());
    }

    // Delay (to make sure the player's gone)
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      for (Player t : Bukkit.getOnlinePlayers()) {
        // Update all online player's sidebar
        updateSidebar(t);
      }
    }, EVENT_DELAY);
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent e) {
    // Only listen for players
    if (!(e.getEntity() instanceof Player p))
      return;

    updateBelowName(p, Math.max(0F, p.getHealth() - e.getDamage()), p.getLevel());
  }

  @EventHandler
  public void onRegainHealth(EntityRegainHealthEvent e) {
    // Only listen for players
    if (!(e.getEntity() instanceof Player p))
      return;

    // Get the maximum health of this player
    AttributeInstance healthAttr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
    double maxHealth = healthAttr == null ? 20.0F : healthAttr.getValue();

    // Constrain health
    double health = Math.min(maxHealth, p.getHealth() + e.getAmount());

    // Propagate this health update to all other players
    updateBelowName(p, health, p.getLevel());
  }

  @EventHandler
  public void onExpChangeEvent(PlayerExpChangeEvent e) {
    Player p = e.getPlayer();

    // How many exp are still missing until the level increases
    // getExp() ranges from 0 to 1, indicating the percentage of the exp bar
    // getExpToLevel() states the whole amount of Exp needed for achieving the next level
    // 1 - getExp() is how much is missing percentually, * getExpToLevel() is in exp
    float missingExp = (1 - p.getExp()) * p.getExpToLevel();
    float recvExp = e.getAmount();
    int currExp = p.getLevel();

    // The player has just leveled up
    if (recvExp >= missingExp)
      currExp++;

    levelsChanged(e.getPlayer(), currExp);
  }

  @EventHandler
  public void onEnchant(EnchantItemEvent e) {
    updateBelowNameDelayed(e.getEnchanter());
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent e) {
    updateBelowNameDelayed(e.getPlayer());
  }

  @EventHandler
  public void onInvClick(InventoryClickEvent e) {
    // Only listen for players
    if (!(e.getWhoClicked() instanceof Player p))
      return;

    // Not an anvil
    if (!(e.getInventory() instanceof AnvilInventory ai))
      return;

    InventoryView ev = e.getView();
    int slot = ev.convertSlot(e.getRawSlot());

    // Not the third slot of the anvil
    if (slot != 2)
      return;

    ItemStack result = ev.getItem(slot);

    // Something resulted, which means a level delta occurred
    if (result != null && result.getType() != Material.AIR)
      updateBelowNameDelayed(p);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void setBelowNameFlag(Player target, BelowNameFlag flag, boolean active) {
    // Start out with a zero-value bitmask
    int curr = belowNameFlags.getOrDefault(target, 0);
    int val = flag.getValue();

    // Either set (OR val) or clear (AND NOT(val)) the bit
    belowNameFlags.put(target, active ? curr | val : curr & ~val);

    // Update the below name for this player
    updateBelowName(target, target.getHealth(), target.getLevel());
  }

  @Override
  public void updateBelowName(Player target) {
    updateBelowName(target, target.getHealth(), target.getLevel());
  }

  @Override
  public void initialize() {
    // Loop all online players
    for (Player t : Bukkit.getOnlinePlayers()) {
      createSidebarObjective(t);

      // Loop all other players relative to t
      for (Player t2 : Bukkit.getOnlinePlayers()) {
        // Skip self
        if (t2 == t)
          continue;

        // Create the below name objective for client t for all other players
        createBelowNameObjective(t, t2);
      }
    }
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
   * Call this routine whenever a player's levels might have changed, as
   * changes only propagate to the sending stage on actual delta
   * @param t Target player
   * @param levels Player's levels
   */
  private void levelsChanged(Player t, int levels) {
    Integer prevExp = prevLevels.get(t);

    // No integer delta occurred
    if (prevExp != null && prevExp == levels)
      return;

    // Propagate this level update to all other players
    updateBelowName(t, t.getHealth(), levels);

    // Update cache
    prevLevels.put(t, levels);
  }

  /**
   * Clear all previously created objectives for a player
   * @param t Target player
   */
  private void clearObjectives(Player t) {
    // Remove the sidebar
    oComm.sendObjective(t, NAME_SIDEBAR, ObjectiveMode.REMOVE, null, null);

    // Clear all below name objectives
    if (knownBelowNames.containsKey(t)) {
      // Iterate in reverse to avoid concurrent modifications
      // as this sub-call also removes the entry
      List<Player> knowns = knownBelowNames.get(t);
      for (int i = knowns.size() - 1; i >= 0; i--)
        clearBelowNameObjective(t, knowns.get(i));
    }
  }

  /**
   * Create the sidebar objective for further use for a player
   * @param t Target player
   */
  private void createSidebarObjective(Player t) {
    // Create the sidebar objective
    if (oComm.sendObjective(
      t, NAME_SIDEBAR,
      ObjectiveMode.CREATE,
      cfg.get(ConfigKey.SIDEBAR_TITLE).asScalar(),
      ObjectiveUnit.INTEGER
    )) {
      // Display the sidebar objective
      oComm.displayObjective(t, NAME_SIDEBAR, ObjectivePosition.SIDEBAR);

      // Display the initial values
      updateSidebar(t);
    }
  }

  /**
   * Create a below-name objective for further use for a player
   * @param t Player to create the objective for
   * @param who Who owns this text
   */
  private void createBelowNameObjective(Player t, Player who) {
    // Create initial empty list
    if (!this.knownBelowNames.containsKey(t))
      this.knownBelowNames.put(t, new ArrayList<>());

    // Don't re-create out already known objectives
    List<Player> known = knownBelowNames.get(t);
    if (known.contains(who))
      return;

    // Create the below name objective
    if (oComm.sendObjective(
      t,
      NAME_BELOW_NAME.formatted(who.getName()),
      ObjectiveMode.CREATE,
      buildBelowNameText(who.getHealth(), belowNameFlags.getOrDefault(who, 0)),
      ObjectiveUnit.INTEGER
    )) {

      // Display the below name objective
      oComm.displayObjective(t, NAME_BELOW_NAME.formatted(who.getName()), ObjectivePosition.BELOW_NAME);

      // Register this below name holder
      known.add(who);
    }

    // Display the initial values
    updateBelowName(who, who.getHealth(), who.getLevel());
  }

  /**
   * Clear a below-name objective for further use for a player
   * @param t Target player
   * @param who Who owns this text
   */
  private void clearBelowNameObjective(Player t, Player who) {
    List<Player> knowns = this.knownBelowNames.get(t);

    // Who is not a registered objective
    if (knowns == null || !knowns.contains(who))
      return;

    // Send remove request
    oComm.sendObjective(
      t,
      NAME_BELOW_NAME.formatted(who.getName()),
      ObjectiveMode.REMOVE,
      null, null
    );

    // Remove this below name holder
    knowns.remove(who);
  }

  /**
   * Build the text that's shown below the playername (used for creation and updating)
   * @param health Health of who
   * @param flags Active {@link BelowNameFlag} bitmask
   */
  private String buildBelowNameText(double health, int flags) {
    String flagStr = Arrays.stream(BelowNameFlag.values())
      .filter(flag -> (flags & flag.getValue()) > 0)
      .map(flag -> flag.getColor().toString() + flag.getSymbol())
      .collect(Collectors.joining(
        cfg.get(ConfigKey.BELOWNAME_FLAGS_JOIN).asScalar()
      ));

    // Format hearts to two decimals
    String hearts = String.valueOf(Math.floor(health * 100) / 100);
    String text = cfg.get(ConfigKey.BELOWNAME_TEXT)
      .withVariable("hearts", hearts)
      .asScalar();

    // Return either the text alone or the text + the flag separator + the flags
    return text + (flagStr.isBlank() ? "" : cfg.get(ConfigKey.BELOWNAME_FLAGS_SEP).asScalar() + flagStr);
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
   * Update the sidebar objective for a player
   * @param t Target player
   */
  private void updateSidebar(Player t) {
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
        oComm.updateScore(t, NAME_SIDEBAR, prev, true, null);
    }

    // Set previous lines cache
    prevSidebarLines.put(t, scores);

    // Update all scores and set their inverse score (since the board is ordered descending)
    for (int i = 0; i < scores.size(); i++)
      oComm.updateScore(t, NAME_SIDEBAR, scores.get(i), false, scores.size() - 1 - i);
  }

  /**
   * Update the below name objective for a player after a specified amount of time passed
   * @param who Player to update the text for
   */
  private void updateBelowNameDelayed(Player who) {
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      updateBelowName(who, who.getHealth(), who.getLevel());
    }, EVENT_DELAY);
  }

  /**
   * Update the below name objective regarding a player who's stats are displayed on it
   * and send the update to all other online players
   * @param who Player to update the text for
   * @param health Health of who
   * @param level Level of who
   */
  private void updateBelowName(Player who, double health, int level) {
    int flags = belowNameFlags.getOrDefault(who, 0);

    for (Player t : Bukkit.getOnlinePlayers()) {
      // Skip self
      if (t == who)
        continue;

      // Update the health score
      if (oComm.updateScore(
        t,
        NAME_BELOW_NAME.formatted(who.getName()),
        who.getName(),
        false,
        level
      )) {

        // Update the below name objective's text
        oComm.sendObjective(
          t,
          NAME_BELOW_NAME.formatted(who.getName()),
          ObjectiveMode.MODIFY_TEXT,
          buildBelowNameText(health, flags),
          ObjectiveUnit.INTEGER
        );
      }
    }
  }
}
