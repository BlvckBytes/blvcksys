package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.handlers.gui.CrateDrawLayout;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.CrateItemModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import me.blvckbytes.blvcksys.persistence.models.SequenceSortResult;
import net.minecraft.util.Tuple;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface ICrateHandler {

  /**
   * Creates a new empty crate
   * @param creator Creating player
   * @param name Name of the crate
   * @return True on success, false if a crate with this name already existed
   */
  boolean createCrate(Player creator, String name);

  /**
   * Delete a crate by it's name
   * @param name Name of the crate
   * @return True on success, false if there was no crate with this name
   */
  boolean deleteCrate(String name);

  /**
   * Move a crate by it's name to a new location
   * @param name Name of the crate
   * @param loc New location, null to delete the location value
   * @return True on success, false if there was no crate with this name
   */
  boolean moveCrate(String name, @Nullable Location loc);

  /**
   * Change a crate's drawing layout
   * @param name Name of the crate
   * @param layout Layout to use when drawing
   * @return True on success, false if there was no crate with this name
   */
  boolean setCrateLayout(String name, @Nullable CrateDrawLayout layout);

  /**
   * Get a crate by it's name
   * @param name Name of the crate
   * @return Optional crate, empty if there was no crate with this name
   */
  Optional<CrateModel> getCrate(String name);

  /**
   * List all existing crates and their items
   */
  List<Tuple<CrateModel, List<CrateItemModel>>> listCrates();

  /**
   * Add a new item with a given probability to an existing crate
   * @param creator Creating player
   * @param crateName Name of the crate
   * @param item Item to add
   * @param probability Probability of this item in percent, ranges from 1 to 100
   * @return True on success, false if there was no crate with this name
   */
  boolean addItem(Player creator, String crateName, ItemStack item, double probability);

  /**
   * Get all items associated with a given crate
   * @param name Name of the crate
   * @return Optional list of items, empty if there was no crate with this name
   */
  Optional<List<CrateItemModel>> getItems(String name);

  /**
   * Delete a crate item by reference
   * @param item Item to delete
   * @return True on success, false if the item didn't exist anymore
   */
  boolean deleteItem(CrateItemModel item);

  /**
   * Sort all items of an existing crate to the sequence of the
   * provided item IDs, where 0 is the first item and n is the last
   * item, handled as currently stored in persistence. All n IDs
   * have to be present for this action to result in a success.
   * @param crateName Name of the crate
   * @param lineIdSequence Sequence of item-IDs in the desired order
   * @return Zero on success, number of missing IDs when missing IDs
   */
  Tuple<SequenceSortResult, Integer> sortItems(String crateName, int[] lineIdSequence) throws PersistenceException;
}
