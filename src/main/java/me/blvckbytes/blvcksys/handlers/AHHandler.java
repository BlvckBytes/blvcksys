package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.AHStateModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/10/2022

  Handles storing the main AH GUI's state as well as all auctions, bids,
  returned items and the like.
 */
@AutoConstruct
public class AHHandler implements IAHHandler, Listener {

  private final IPersistence pers;
  private final Map<Player, AHStateModel> stateCache;

  public AHHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
    this.stateCache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public AHStateModel getState(OfflinePlayer p) {
    // Respond from cache
    if (p instanceof Player onP && stateCache.containsKey(onP))
      return stateCache.get(onP);

    // Try to fetch from DB
    return pers.findFirst(buildQuery(p))
      // Not in DB yet
      .orElseGet(() -> {
        // Create, store and cache a new default instance
        AHStateModel def = AHStateModel.makeDefault(p);
        storeState(def);

        // Don't cache offline players
        if (p instanceof Player onP)
          stateCache.put(onP, def);

        return def;
      });
  }

  @Override
  public void storeState(AHStateModel state) {
    pers.store(state);
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    stateCache.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Builds a query to select the state of a player
   * @param owner Target player
   */
  private QueryBuilder<AHStateModel> buildQuery(OfflinePlayer owner) {
    return new QueryBuilder<>(
      AHStateModel.class,
      "owner__uuid", EqualityOperation.EQ, owner.getUniqueId()
    );
  }
}
