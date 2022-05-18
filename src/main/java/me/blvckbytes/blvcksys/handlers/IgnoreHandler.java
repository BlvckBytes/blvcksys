package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.PlayerIgnoreModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldQueryGroup;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/19/2022

  Handles ignoring players on different levels with one ignore model per
  executing player and provides an abstracted API.
 */
@AutoConstruct
public class IgnoreHandler implements IIgnoreHandler, Listener {

  // Default values of all ignores
  private final static boolean DEF_MSG_IGNORE = false;
  private final static boolean DEF_CHAT_IGNORE = false;

  private final Set<OfflinePlayer> cached;
  private final Map<OfflinePlayer, PlayerIgnoreModel> cache;
  private final IPersistence pers;

  public IgnoreHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
    this.cache = new HashMap<>();
    this.cached = new HashSet<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public boolean getChatIgnore(OfflinePlayer executor, OfflinePlayer target) throws PersistenceException {
    return getModel(executor, target)
      .map(PlayerIgnoreModel::isIgnoresChat)
      .orElse(DEF_CHAT_IGNORE);
  }

  @Override
  public boolean getMsgIgnore(OfflinePlayer executor, OfflinePlayer target) throws PersistenceException {
    return getModel(executor, target)
      .map(PlayerIgnoreModel::isIgnoresMsg)
      .orElse(DEF_MSG_IGNORE);
  }

  @Override
  public void setChatIgnore(OfflinePlayer executor, OfflinePlayer target, boolean value) throws PersistenceException {
    PlayerIgnoreModel model = getModelOrCreate(executor, target);
    model.setIgnoresChat(value);
    pers.store(model);
  }

  @Override
  public void setMsgIgnore(OfflinePlayer executor, OfflinePlayer target, boolean value) throws PersistenceException {
    PlayerIgnoreModel model = getModelOrCreate(executor, target);
    model.setIgnoresMsg(value);
    pers.store(model);
  }

  //=========================================================================//
  //                                Listener                                 //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    cache.remove(e.getPlayer());
    cached.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Get an existing ignore model from either local cache or persistence and
   * create a new model using default values when it doesn't exist
   * @param executor Executing player
   * @param target Player that is targetted by these ignores
   * @return Player ignore model
   */
  private PlayerIgnoreModel getModelOrCreate(OfflinePlayer executor, OfflinePlayer target) {
    Optional<PlayerIgnoreModel> res = getModel(executor, target);

    if (res.isPresent())
      return res.get();

    PlayerIgnoreModel model = new PlayerIgnoreModel(
      executor, target,
      DEF_CHAT_IGNORE,
      DEF_MSG_IGNORE
    );

    pers.store(model);
    cache.put(executor, model);
    cached.add(executor);

    return model;
  }

  /**
   * Get an existing ignore model from either local cache or persistence
   * @param executor Executing player
   * @param target Player that is targetted by these ignores
   * @return Model if it exists, empty otherwise
   */
  private Optional<PlayerIgnoreModel> getModel(OfflinePlayer executor, OfflinePlayer target) throws PersistenceException {
    if (cached.contains(executor))
      return cache.containsKey(executor) ? Optional.of(cache.get(executor)) : Optional.empty();

    cached.add(executor);
    return pers.findFirst(buildQuery(executor, target))
      .map(res -> {
        cache.put(executor, res);
        return res;
      });
  }

  /**
   * Build a query which selects the ignore model for the provided pair of players
   * @param executor Executing player
   * @param target Player that is targetted by these ignores
   */
  private QueryBuilder<PlayerIgnoreModel> buildQuery(OfflinePlayer executor, OfflinePlayer target) {
    return new QueryBuilder<>(
      PlayerIgnoreModel.class,
      new FieldQueryGroup("creator__uuid", EqualityOperation.EQ, executor.getUniqueId())
        .and("target__uuid", EqualityOperation.EQ, target.getUniqueId())
    );
  }
}
