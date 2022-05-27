package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Handles managing a crate and it's associated list of items.
*/
@AutoConstruct
public class CrateHandler implements ICrateHandler {

  private final Map<String, CrateModel> crateCache;
  private final Map<String, List<CrateItemModel>> itemsCache;

  private final IPersistence pers;

  public CrateHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
    this.crateCache = new HashMap<>();
    this.itemsCache = new HashMap<>();
  }

  @Override
  public boolean createCrate(Player creator, String name, @Nullable Location loc) {
    try {
      CrateModel crate = new CrateModel(creator, name, loc);
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
  public boolean addItem(Player creator, String crateName, ItemStack item, double probability) {
    CrateModel crate = getCrate(crateName).orElse(null);

    if (crate == null)
      return false;

    CrateItemModel itemModel = new CrateItemModel(crate.getId(), creator, item, probability);
    pers.store(itemModel);

    if (!itemsCache.containsKey(crateName.toLowerCase()))
      itemsCache.put(crateName.toLowerCase(), new ArrayList<>());
    itemsCache.get(crateName.toLowerCase()).add(itemModel);

    return true;
  }

  @Override
  public Optional<List<CrateItemModel>> getItems(String name) {
    if (itemsCache.containsKey(name.toLowerCase()))
      return Optional.of(itemsCache.get(name.toLowerCase()));

    CrateModel crate = getCrate(name).orElse(null);
    if (crate == null)
      return Optional.empty();

    List<CrateItemModel> items = pers.find(buildCrateItemsQuery(crate.getId()));
    itemsCache.put(name, items);
    return Optional.of(items);
  }

  @Override
  public boolean deleteItem(CrateItemModel item) {
    itemsCache.values().forEach(items -> items.remove(item));
    return pers.delete(item);
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

  private QueryBuilder<CrateItemModel> buildCrateItemsQuery(UUID crateId) {
    return new QueryBuilder<>(
      CrateItemModel.class,
      "crateId", EqualityOperation.EQ, crateId
    );
  }

  private QueryBuilder<CrateModel> buildCrateQuery(String name) {
    return new QueryBuilder<>(
      CrateModel.class,
      "name", EqualityOperation.EQ_IC, name
    );
  }
}
