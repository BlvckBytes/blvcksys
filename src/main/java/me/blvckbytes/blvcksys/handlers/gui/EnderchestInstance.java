package me.blvckbytes.blvcksys.handlers.gui;

import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.models.EnderchestModel;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Represents an active instance of a player's enderchest which may be shared
  and synced accross multiple enderchest viewers. Whenever a viewer makes changes
  to this enderchest, they call changed(), which then broadcasts the change to
  all other viewers, so they can update their view and retrieve the latest state
  of the current page's inventory from the model, which is being synced into.
*/
public class EnderchestInstance {

  @Getter
  private final EnderchestModel model;

  // Mapping enderchest viewers to their after changes callback
  private final Map<Player, Consumer<Player>> afterChanges;

  private final AtomicBoolean changes;

  /**
   * Create a new instance based on a loaded model
   * @param model Model to base off of
   */
  public EnderchestInstance(EnderchestModel model) {
    this.model = model;
    this.changes = new AtomicBoolean(false);
    this.afterChanges = new HashMap<>();
  }

  /**
   * To be invoked after changes have been applied and synced into the model
   * @param viewer Viewer that caused the changes
   */
  public void changed(Player viewer) {
    this.changes.set(true);

    for (Map.Entry<Player, Consumer<Player>> e : afterChanges.entrySet()) {
      // Don't call change callbacks on self
      if (e.getKey().equals(viewer))
        continue;

      e.getValue().accept(viewer);
    }
  }

  /**
   * To be invoked after changes have been stored persistently
   */
  public void stored() {
    this.changes.set(false);
  }

  /**
   * Register a new callback to be invoked after changes
   * @param viewer Viewer which registers the callback
   * @param changes Callback
   */
  public void registerAfterChanges(Player viewer, Consumer<Player> changes) {
    unregisterAfterChanges(viewer);
    afterChanges.put(viewer, changes);
  }

  /**
   * Unregister an existing changes callback
   * @param viewer Viewer which registered the callback
   */
  public void unregisterAfterChanges(Player viewer) {
    afterChanges.remove(viewer);
  }

  /**
   * Checks whether this enderchest is currently used by anybody
   */
  public boolean isInUse() {
    return this.afterChanges.size() > 0;
  }

  /**
   * Checks whether this enderchest has any unsaved changes
   */
  public boolean hasChanges() {
    return changes.get();
  }
}
