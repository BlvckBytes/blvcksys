package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.handlers.gui.AnimationType;
import me.blvckbytes.blvcksys.handlers.gui.CrateContentGui;
import me.blvckbytes.blvcksys.handlers.gui.CrateDrawGui;
import me.blvckbytes.blvcksys.handlers.gui.CrateDrawLayout;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.CrateItemModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import me.blvckbytes.blvcksys.persistence.models.SequenceSortResult;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import net.minecraft.util.Tuple;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Handles managing a crate and it's associated list of items.
*/
@AutoConstruct
public class CrateHandler implements ICrateHandler, Listener, IAutoConstructed {

  private static final Random rand = new Random();

  private final Map<String, CrateModel> crateCache;
  private final Map<String, List<CrateItemModel>> itemsCache;
  private final Map<String, Tuple<Double, List<Double>>> probabilityRanges;

  @AutoInjectLate
  private CrateDrawGui drawGui;

  @AutoInjectLate
  private CrateContentGui contentGui;

  private final IPersistence pers;

  public CrateHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
    this.crateCache = new HashMap<>();
    this.itemsCache = new HashMap<>();
    this.probabilityRanges = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public boolean createCrate(Player creator, String name) {
    try {
      CrateModel crate = new CrateModel(creator, name, null, null);
      pers.store(crate);
      crateCache.put(name.toLowerCase(), crate);
      return true;
    } catch (DuplicatePropertyException e) {
      return false;
    }
  }

  @Override
  public boolean deleteCrate(String name) {
    crateCache.remove(name.toLowerCase());
    return pers.delete(buildCrateQuery(name)) > 0;
  }

  @Override
  public boolean moveCrate(String name, @Nullable Location loc) {
    CrateModel crate = getCrate(name).orElse(null);

    if (crate == null)
      return false;

    crate.setLoc(loc);
    pers.store(crate);
    return true;
  }

  @Override
  public boolean setCrateLayout(String name, @Nullable CrateDrawLayout layout) {
    CrateModel crate = getCrate(name).orElse(null);

    if (crate == null)
      return false;

    crate.setLayout(layout);
    pers.store(crate);
    return true;
  }

  @Override
  public Optional<CrateModel> getCrate(String name) {
    if (crateCache.containsKey(name.toLowerCase()))
      return Optional.of(crateCache.get(name.toLowerCase()));

    return pers.findFirst(buildCrateQuery(name))
      .map(crate -> {
        crateCache.put(name.toLowerCase(), crate);
        return crate;
      });
  }

  @Override
  public List<Tuple<CrateModel, List<CrateItemModel>>> listCrates() {
    return pers.list(CrateModel.class).stream()
      .map(crate -> new Tuple<>(crate, getItems(crate.getName()).orElse(new ArrayList<>())))
      .toList();
  }

  @Override
  public boolean addItem(Player creator, String crateName, ItemStack item, double probability) {
    CrateModel crate = getCrate(crateName).orElse(null);

    if (crate == null)
      return false;

    CrateItemModel itemModel = new CrateItemModel(crate.getId(), creator, item, probability);
    CrateItemModel.pushSequenceMember(itemModel, buildCrateItemsQuery(crate.getId()), pers);

    if (!itemsCache.containsKey(crateName.toLowerCase()))
      itemsCache.put(crateName.toLowerCase(), new ArrayList<>());
    itemsCache.get(crateName.toLowerCase()).add(itemModel);

    buildProbabilityRanges(crateName);
    return true;
  }

  @Override
  public Optional<List<CrateItemModel>> getItems(String name) {
    if (itemsCache.containsKey(name.toLowerCase()))
      return Optional.of(itemsCache.get(name.toLowerCase()));

    CrateModel crate = getCrate(name).orElse(null);
    if (crate == null)
      return Optional.empty();

    List<CrateItemModel> items = CrateItemModel.sequentize(pers.find(buildCrateItemsQuery(crate.getId())));
    itemsCache.put(name, items);
    return Optional.of(items);
  }

  @Override
  public boolean deleteItem(CrateItemModel item) {
    itemsCache.values().forEach(items -> items.remove(item));
    findCrateById(item.getCrateId()).ifPresent(crate -> buildProbabilityRanges(crate.getName()));
    return CrateItemModel.deleteSequenceMember(item, pers);
  }

  @Override
  public boolean updateItem(CrateItemModel item) {
    pers.store(item);
    findCrateById(item.getCrateId()).ifPresent(crate -> buildProbabilityRanges(crate.getName()));
    return true;
  }

  @Override
  public Tuple<SequenceSortResult, Integer> sortItems(String crateName, int[] itemIdSequence) throws PersistenceException {
    CrateModel crate = getCrate(crateName).orElse(null);
    if (crate == null)
      return new Tuple<>(SequenceSortResult.MODEL_UNKNOWN, 0);

    Tuple<SequenceSortResult, Integer> res = CrateItemModel.alterSequence(buildCrateItemsQuery(crate.getId()), itemIdSequence, pers);

    if (res.a() == SequenceSortResult.SORTED)
      itemsCache.put(crateName.toLowerCase(), pers.find(buildCrateItemsQuery(crate.getId())));

    return res;
  }

  @Override
  public Optional<CrateItemModel> drawItem(String crateName) {
    List<CrateItemModel> items = itemsCache.get(crateName);
    Tuple<Double, List<Double>> probData = probabilityRanges.get(crateName);

    if (probData == null || items == null)
      return Optional.empty();

    double targetRange = rand.nextDouble() * probData.a();
    for (int i = 0; i < probData.b().size(); i++) {
      if (probData.b().get(i) >= targetRange)
        return Optional.of(items.get(i));
    }

    return Optional.empty();
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @EventHandler
  public void onBreak(BlockBreakEvent e) {
    if (crateCache.values().stream().anyMatch(crate -> crate.getLoc().equals(e.getBlock().getLocation())))
      e.setCancelled(true);
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    if (e.getClickedBlock() == null)
      return;

    Location l = e.getClickedBlock().getLocation();
    CrateModel targetCrate = crateCache.values().stream()
      .filter(crate -> crate.getLoc().equals(l))
      .findFirst()
      .orElse(null);

    if (targetCrate == null)
      return;

    e.setCancelled(true);

    // Open the crate and start a draw
    // TODO: Check for keys
    if (e.getAction() == Action.RIGHT_CLICK_BLOCK && drawGui != null)
      drawGui.show(e.getPlayer(), targetCrate, AnimationType.SLIDE_RIGHT);

    // Show the crate contents
    else if (e.getAction() == Action.LEFT_CLICK_BLOCK && contentGui != null)
      contentGui.show(e.getPlayer(), new Tuple<>(targetCrate, false), AnimationType.SLIDE_UP);
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Build a query to select a crate's items by their crate ID
   * @param crateId ID of the target crate
   */
  private QueryBuilder<CrateItemModel> buildCrateItemsQuery(UUID crateId) {
    return new QueryBuilder<>(
      CrateItemModel.class,
      "crateId", EqualityOperation.EQ, crateId
    );
  }

  /**
   * Build a query to select a crate by it's name
   * @param name Name of the target crate
   */
  private QueryBuilder<CrateModel> buildCrateQuery(String name) {
    return new QueryBuilder<>(
      CrateModel.class,
      "name", EqualityOperation.EQ_IC, name
    );
  }

  /**
   * Find a crate by it's ID within the cache
   * @param id ID of the target crate
   */
  private Optional<CrateModel> findCrateById(UUID id) {
    return crateCache.values().stream().filter(c -> c.getId().equals(id)).findFirst();
  }

  /**
   * Build probability ranges for all items of the target crate
   * @param name Name of the crate
   */
  private void buildProbabilityRanges(String name) {
    List<CrateItemModel> items = itemsCache.get(name.toLowerCase());
    if (items == null)
      return;

    List<Double> ranges = new ArrayList<>();
    double accumulator = 0;
    for (CrateItemModel item : items) {
      accumulator += item.getProbability();
      ranges.add(accumulator);
    }

    probabilityRanges.put(name.toLowerCase(), new Tuple<>(accumulator, ranges));
  }

  @Override
  public void cleanup() {}

  @Override
  public void initialize() {
    // Load all creates and all items on start
    pers.list(CrateModel.class).forEach(crate -> crateCache.put(crate.getName().toLowerCase(), crate));
    crateCache.keySet().forEach(this::getItems);
    crateCache.keySet().forEach(this::buildProbabilityRanges);
  }
}
