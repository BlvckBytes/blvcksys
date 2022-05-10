package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.packets.communicators.hologram.IHologramCommunicator;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.ModelNotFoundException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.HologramLineModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldQueryGroup;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Handles managing a hologram's list of lines, which is built by a
  doubly linked list and thus provides an easy API which abstracts
  this underlying fact for all callers.
*/
@AutoConstruct
public class HologramHandler implements IHologramHandler, IAutoConstructed {

  // Specifies the time between hologram update triggers in ticks
  private static final long UPDATE_INTERVAL_TICKS = 20;

  // Local cache for hologram lines, mapping the hologram name to a list of lines
  // This is crucial, as holograms will be accessed a lot for drawing, updating, commands, ...
  private final Map<String, List<HologramLineModel>> cache;
  private final Map<String, MultilineHologram> holograms;
  private int intervalHandle;

  private final IPersistence pers;
  private final JavaPlugin plugin;
  private final IHologramCommunicator holoComm;
  private final IHologramVariableSupplier varSupp;

  public HologramHandler(
    @AutoInject IPersistence pers,
    @AutoInject JavaPlugin plugin,
    @AutoInject IHologramCommunicator holoComm,
    @AutoInject IHologramVariableSupplier varSupp
  ) {
    this.pers = pers;
    this.plugin = plugin;
    this.holoComm = holoComm;
    this.intervalHandle = -1;
    this.varSupp = varSupp;

    this.cache = new HashMap<>();
    this.holograms = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public Tuple<HologramSortResult, Integer> sortHologramLines(String name, int[] lineIdSequence) {
    List<HologramLineModel> lines = getHologramLines(name)
      .orElseThrow(() -> new ModelNotFoundException(HologramLineModel.class.getSimpleName(), name));

    // There are some IDs missing
    if (lines.size() > lineIdSequence.length)
      return new Tuple<>(HologramSortResult.IDS_MISSING, lines.size() - lineIdSequence.length);

    // Sort the holograms as specified by the ID-list
    List<HologramLineModel> sorted = new ArrayList<>();
    for (int sequenceId : lineIdSequence) {
      if (sequenceId <= 0 || sequenceId > lines.size())
        return new Tuple<>(HologramSortResult.ID_INVALID, sequenceId);
      sorted.add(lines.get(sequenceId - 1));
    }

    // Change linked list pointers accordingly
    for (int i = 0; i < sorted.size(); i++) {
      HologramLineModel curr = sorted.get(i);
      curr.setPrevious(i == 0 ? null : sorted.get(i - 1).getId());
      curr.setNext(i == sorted.size() - 1 ? null : sorted.get(i + 1).getId());
      pers.store(curr);
    }

    // Load the changes into cache
    getHologramLines(name, true);

    return new Tuple<>(HologramSortResult.SORTED, 0);
  }

  @Override
  public boolean deleteHologram(String name) throws PersistenceException {
    // Remove this entry from the cache
    this.cache.remove(name.toLowerCase());

    // Remove this entry from the list of active holograms and destroy it for all players
    this.holograms.remove(name.toLowerCase()).destroy();

    return pers.delete(
      new QueryBuilder<>(
        HologramLineModel.class,
        "name", EqualityOperation.EQ_IC, name
      )
    ) > 0;
  }

  @Override
  public boolean moveHologram(String name, Location loc) throws PersistenceException {
    List<HologramLineModel> lines = getHologramLines(name)
      .orElse(null);

    // Hologram did not exist
    if (lines == null)
      return false;

    // Update the location for all lines
    for (HologramLineModel line : lines) {
      line.setLoc(loc);
      pers.store(line);
    }

    // Load the changes into cache
    getHologramLines(name, true);

    return true;
  }

  @Override
  public boolean deleteHologramLine(HologramLineModel line) throws PersistenceException {
    // Find the predecessor which points to the line by "next"
    HologramLineModel predecessor = pers.findFirst(
      new QueryBuilder<>(
        HologramLineModel.class,
        "next", EqualityOperation.EQ, line.getId()
      )
    ).orElse(null);

    // Find the successor which points to the line by "previous"
    HologramLineModel successor = pers.findFirst(
      new QueryBuilder<>(
        HologramLineModel.class,
        "previous", EqualityOperation.EQ, line.getId()
      )
    ).orElse(null);

    // Node inbetween two other elements
    if (predecessor != null && successor != null) {
      // The predecessor now points to the successor for next
      // and the successor now points to the predecessor for previous,
      // effectively skipping the node to delete
      predecessor.setNext(successor.getId());
      successor.setPrevious(predecessor.getId());

      pers.store(predecessor);
      pers.store(successor);
    }

    // No successor, is a tail node
    else if (predecessor != null) {
      // Now the predecessor becomes the new tail node
      predecessor.setNext(null);
      pers.store(predecessor);
    }

    // No predecessor, is a head node
    else if (successor != null) {
      // Now the successor becomes the new head node
      successor.setPrevious(null);
      pers.store(successor);
    }

    // Otherwise: was the only node, nothing to change

    try {
      String name = line.getName();

      // Delete the item which is now not used in
      // any other foreign keys anymore
      pers.delete(line);

      // Load the changes into cache
      getHologramLines(name, true);
      return true;
    } catch (ModelNotFoundException e) {
      return false;
    }
  }

  @Override
  public HologramLineModel createHologramLine(OfflinePlayer creator, String name, Location loc, String text) throws PersistenceException {
    // Find the tail of the current list of lines
    HologramLineModel tail = pers.findFirst(
      new QueryBuilder<>(
        HologramLineModel.class,
        "name", EqualityOperation.EQ_IC, name
      ).and(
        "next", EqualityOperation.EQ, null
      )
    ).orElse(null);

    // Decide whether previous is null (new is first entry) or the tail's ID
    UUID previous = null;
    if (tail != null)
      previous = tail.getId();

    // Create a new line pointing at previous and having no successor
    HologramLineModel line = new HologramLineModel(creator, name, loc, text, previous, null);
    pers.store(line);

    // Update the "previous tail"'s next to the newly created ID
    if (tail != null) {
      tail.setNext(line.getId());
      pers.store(tail);
    }

    // Load the changes into cache
    getHologramLines(name, true);
    return line;
  }

  @Override
  public void changeHologramLine(HologramLineModel line, String newLine) {
    line.setText(newLine);
    pers.store(line);

    // Load the changes into cache
    getHologramLines(line.getName(), true);
  }

  @Override
  public Optional<List<HologramLineModel>> getHologramLines(String name) throws PersistenceException {
    // Return cached responses
    return getHologramLines(name, false);
  }

  @Override
  public Map<String, List<HologramLineModel>> getNear(Location where, double rangeRadius) throws PersistenceException {
    // This should never be the case...
    if (where.getWorld() == null)
      throw new PersistenceException("Cannot find any near holograms if no world has been provided");

    Map<String, List<HologramLineModel>> ret = new HashMap<>();
    List<HologramLineModel> res = pers.find(
      new QueryBuilder<>(
        // Has to be in the same world
        HologramLineModel.class, "loc__world", EqualityOperation.EQ, where.getWorld().getName()
      )
        // X range constraint
        .and(
          new FieldQueryGroup("loc__x", EqualityOperation.GTE, where.getX() - rangeRadius)
            .and("loc__x", EqualityOperation.LTE, where.getX() + rangeRadius)
        )

        // Y range constraint
        .and(
          new FieldQueryGroup("loc__y", EqualityOperation.GTE, where.getY() - rangeRadius)
            .and("loc__y", EqualityOperation.LTE, where.getY() + rangeRadius)
        )

        // Z range constraint
        .and(
          new FieldQueryGroup("loc__z", EqualityOperation.GTE, where.getZ() - rangeRadius)
            .and("loc__z", EqualityOperation.LTE, where.getZ() + rangeRadius)
        )
    );

    // Group lines by their name for convenience
    for (HologramLineModel line : res) {
      // Create empty lists initially
      if (!ret.containsKey(line.getName()))
        ret.put(line.getName(), new ArrayList<>());

      // Add the line to it's "name group"
      ret.get(line.getName()).add(line);
    }

    return ret;
  }

  @Override
  public void cleanup() {
    // Stop the ticker task
    if (this.intervalHandle > 0)
      Bukkit.getScheduler().cancelTask(this.intervalHandle);

    // Destroy all created holograms
    for (MultilineHologram holo : holograms.values())
      holo.destroy();
  }

  @Override
  public void initialize() {
    // Load all existing holograms into memory on load
    loadAllHolograms();

    // Start the ticker interval
    this.intervalHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {

      long time = 0;

      @Override
      public void run() {
        for (MultilineHologram holo : holograms.values())
          holo.tick(time);
        time += UPDATE_INTERVAL_TICKS;
      }
    }, 0L, UPDATE_INTERVAL_TICKS);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Get all lines a hologram holds and cache results
   * @param name Name of the hologram
   * @param invalidateCache Whether or not to invalidate the cache and force an update
   * @return Optional list of lines, empty if this hologram didn't yet exist
   */
  private Optional<List<HologramLineModel>> getHologramLines(String name, boolean invalidateCache) throws PersistenceException {
    // Respond from cache
    if (!invalidateCache && cache.containsKey(name.toLowerCase()))
      return Optional.of(cache.get(name.toLowerCase()));

    List<HologramLineModel> unsortedLines = pers.find(
      new QueryBuilder<>(
        HologramLineModel.class,
        "name", EqualityOperation.EQ_IC, name
      )
    );

    // This hologram doesn't yet exist
    if (unsortedLines.size() == 0)
      return Optional.empty();

    List<HologramLineModel> sortedLines = HologramLineModel.sequentize(unsortedLines);

    // Keep the hologram instances in sync
    createOrUpdateHolograms(name, sortedLines);

    // Cache result
    this.cache.put(name.toLowerCase(), sortedLines);
    return Optional.of(sortedLines);
  }

  /**
   * Load all globally existing hologram lines into memory, group them, sort them and
   * write them into the cache. Then, create {@link MultilineHologram}'s for all names.
   */
  private void loadAllHolograms() {
    List<HologramLineModel> lines = pers.list(HologramLineModel.class);
    Map<String, List<HologramLineModel>> groupedLines = new HashMap<>();

    // Group all lines by name locally
    for (HologramLineModel line : lines) {
      String name = line.getName().toLowerCase();

      if (!groupedLines.containsKey(name))
        groupedLines.put(name, new ArrayList<>());

      groupedLines.get(name).add(line);
    }

    // Sort all lines and write them into the cache
    for (Map.Entry<String, List<HologramLineModel>> groupedLine : groupedLines.entrySet()) {
      String name = groupedLine.getKey();
      this.cache.put(name, HologramLineModel.sequentize(groupedLine.getValue()));
    }

    // Create initial hologram instances from the cache entries
    for (Map.Entry<String, List<HologramLineModel>> hologram : cache.entrySet())
      createOrUpdateHolograms(hologram.getKey(), hologram.getValue());
  }

  /**
   * Either create a new {@link MultilineHologram} instance to manage the hologram or
   * update an existing instance with the new values
   * @param name Name of the hologram
   * @param lines List of sorted lines to display
   */
  private void createOrUpdateHolograms(String name, List<HologramLineModel> lines) {
    // Transform the hologram lines to their text contents
    List<String> strLines = lines.stream()
      .map(HologramLineModel::getText)
      .toList();

    // Get the location of the first line
    Location loc = lines.get(0).getLoc();

    // Hologram didn't yet exist, create it
    if (!this.holograms.containsKey(name.toLowerCase()))
      this.holograms.put(name.toLowerCase(), new MultilineHologram(name, loc, strLines, holoComm, varSupp));

    // Update the existing hologram
    else {
      MultilineHologram holo = this.holograms.get(name.toLowerCase());
      holo.setLines(strLines);
      holo.setLoc(loc);
    }
  }
}
