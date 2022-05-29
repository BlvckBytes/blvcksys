package me.blvckbytes.blvcksys.handlers;

import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.models.CrateItemModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import net.minecraft.util.Tuple;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Represents a cached crate with it's items and a live probability range map.
*/
public class CachedCrate {

  // Random used for drawing
  private static final Random rand = new Random();

  @Getter
  private final CrateModel crate;
  private Map<CrateItemModel, Double> items;
  private double totalProbability;

  /**
   * Create a new cached crate from it's underlying crate model and a list of items
   * which will be converted into a probability range map
   * @param crate Underlying crate
   * @param items Items of the crate
   */
  public CachedCrate(CrateModel crate, List<CrateItemModel> items) {
    this.crate = crate;
    setItems(items);
  }

  /**
   * Get this crate as a tuple of the crate and a list of all of it's items
   */
  public Tuple<CrateModel, List<CrateItemModel>> asTuple() {
    return new Tuple<>(crate, items.keySet().stream().toList());
  }

  /**
   * Add a new crate item and internally calculate all new probabilities
   * @param item New item to add
   */
  public void addItem(CrateItemModel item) {
    List<CrateItemModel> items = new ArrayList<>(this.items.keySet());
    items.add(item);
    setItems(items);
  }

  /**
   * Remove a crate item if it exists and internally calculate all new probabilities
   * @param item Item to remove
   */
  public void removeItem(CrateItemModel item) {
    if (items.remove(item) != null)
      setItems(items.keySet().stream().toList());
  }

  /**
   * Update the crate by re-calculating all probabilities
   */
  public void update() {
    setItems(items.keySet().stream().toList());
  }

  /**
   * Set new crate items and internally calculate all new probabilities
   * @param items New list of items
   */
  public void setItems(List<CrateItemModel> items) {
    Tuple<Double, Map<CrateItemModel, Double>> probs = buildProbabilityRanges(items);
    this.items = probs.b();
    this.totalProbability = probs.a();
  }

  /**
   * Get all items within this crate
   */
  public List<CrateItemModel> getItems() {
    return new ArrayList<>(items.keySet());
  }

  /**
   * Draw a crate item based on the item probabilities
   * @return Optional item, empty if there were no items to draw from
   */
  public Optional<CrateItemModel> drawItem() {
    // Nothing to draw from
    if (items.size() == 0)
      return Optional.empty();

    // Decide on an item by it's range
    double targetRange = rand.nextDouble() * totalProbability;
    for (CrateItemModel item : items.keySet()) {
      if (items.get(item) >= targetRange)
        return Optional.of(item);
    }

    return Optional.empty();
  }

  /**
   * Build probability ranges for all items
   * @param items Items to build ranges for
   * @return Tuple of the total probability as well as the range map
   */
  private Tuple<Double, Map<CrateItemModel, Double>> buildProbabilityRanges(List<CrateItemModel> items) {
    // Use a linked hash map here to preserve insertion order
    Map<CrateItemModel, Double> ranges = new LinkedHashMap<>();

    double accumulator = 0;
    for (CrateItemModel item : items) {
      accumulator += item.getProbability();
      ranges.put(item, accumulator);
    }

    return new Tuple<>(accumulator, ranges);
  }
}
