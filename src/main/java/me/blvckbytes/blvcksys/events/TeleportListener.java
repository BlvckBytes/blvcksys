package me.blvckbytes.blvcksys.events;

import lombok.NoArgsConstructor;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/03/2022

  Listens to teleportations and constructs a personal
  location history for every player. Histories are kept for
  a specified amount of time after a player disconnects and
  get disposed on expiry automatically.
*/
@AutoConstruct
public class TeleportListener implements Listener, ITeleportListener, IAutoConstructed {

  /**
   * Represents a personal location history
   */
  @NoArgsConstructor
  private static class LocationHistory {
    // Example: ... - PREV - PREV - [CURR] - NEXT - ...
    // History of locations
    private final LinkedList<Location> locs = new LinkedList<>();

    // Last location teleported to
    private Location lastLoc = null;

    // Task handle for auto-deletion timeouts
    private int timeoutHandle = -1;
  }

  // How long to keep histories for after the owner left the server (in seconds)
  private static final int HISTORY_TIMEOUT_S = 60 * 15;

  // Maximal length of the location history per player
  private static final int HISTORY_MAXLEN = 10;

  // Mapping players (their UUIDs) to their personal history
  private final Map<UUID, LocationHistory> histories;

  private final JavaPlugin plugin;

  public TeleportListener(
    @AutoInject JavaPlugin plugin
  ) {
    this.plugin = plugin;
    this.histories = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public Optional<Location> getHistoryNext(Player p) {
    return getLastLocation(p)
      .flatMap(data -> {
        LinkedList<Location> locs = data.a().locs;

        // No next location to offer
        if (locs.size() <= data.b() + 1)
          return Optional.empty();

        // Provide the next location
        return Optional.of(locs.get(data.b() + 1));
      });
  }

  @Override
  public Optional<Location> getHistoryPrevious(Player p) {
    return getLastLocation(p)
      .flatMap(data -> {
        LinkedList<Location> locs = data.a().locs;

        // No previous location to offer
        if (data.b() - 1 < 0)
          return Optional.empty();

        // Provide the previous location
        return Optional.of(locs.get(data.b() - 1));
      });
  }

  @Override
  public void cleanup() {
    // Kill all timeout tasks
    for (LocationHistory history : histories.values()) {
      if (history.timeoutHandle > 0)
        Bukkit.getScheduler().cancelTask(history.timeoutHandle);
    }

    histories.clear();
  }

  @Override
  public void initialize() {}

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler(priority = EventPriority.LOWEST)
  public void onTeleport(PlayerTeleportEvent e) {
    saveInHistory(e.getPlayer(), e.getFrom());
    saveInHistory(e.getPlayer(), e.getTo());
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    LocationHistory history = histories.get(e.getPlayer().getUniqueId());

    // Has no history or it's not in an active timeout
    if (history == null || history.timeoutHandle < 0)
      return;

    // Cancel the timeout task and thus save the history
    Bukkit.getScheduler().cancelTask(history.timeoutHandle);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    UUID u = e.getPlayer().getUniqueId();
    LocationHistory history = histories.get(u);

    // Has no history
    if (history == null)
      return;

    // Create a new deletion timeout task
    history.timeoutHandle = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      histories.remove(u);
    }, HISTORY_TIMEOUT_S * 20);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Get the last location index from a player's history
   * @param p Target player
   * @return History and index, empty if there's no last location
   */
  private Optional<Tuple<LocationHistory, Integer>> getLastLocation(Player p) {
    LocationHistory history = histories.get(p.getUniqueId());

    // Has no history yet
    if (history == null || history.locs.size() == 0)
      return Optional.empty();

    // Has no last location set
    int lastIndex = history.locs.indexOf(history.lastLoc);
    if (lastIndex < 0)
      return Optional.empty();

    return Optional.of(new Tuple<>(history, lastIndex));
  }

  /**
   * Add an occurred teleportation to the player's location history
   * @param p Target player
   * @param loc Location teleported loc
   */
  private void saveInHistory(Player p, Location loc) {

    UUID u = p.getUniqueId();

    // Create histories initially
    if (!histories.containsKey(u))
      histories.put(u, new LocationHistory());

    LocationHistory history = histories.get(u);

    // Search for an existing entry of this location
    Location existing = null;
    for (Location cloc : history.locs) {
      // Locations differ
      if (!compareLocations(cloc, loc))
        continue;

      existing = cloc;
      break;
    }

    // Set the last location (re-using known refs)
    history.lastLoc = existing != null ? existing : loc;

    // Don't keep any duplicates
    if (existing == null)
      history.locs.add(loc);

    // Remove the "oldest" entries as long as the list exceeds the maximum length
    while (history.locs.size() > HISTORY_MAXLEN)
      history.locs.removeFirst();
  }

  /**
   * Compare two locations for equality
   * @param a Location A
   * @param b Location B
   * @return Whether A is equal to B (for the purposes of this application)
   */
  private boolean compareLocations(Location a, Location b) {
    return (
      a.getBlockX() == b.getBlockX() &&
      a.getBlockY() == b.getBlockY() &&
      a.getBlockZ() == b.getBlockZ()
    );
  }
}
