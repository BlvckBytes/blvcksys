package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.PreferencesModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Handles initially creating a preference model entry with sane default
  values and exposes a set of methods to tweak individual settings while
  keeping them in-sync with the persisted model and caching results for fast access.
*/
@AutoConstruct
public class PreferencesHandler implements IPreferencesHandler {

  // Default values of all preferences
  private final static boolean DEF_SCOREBOARD_HIDDEN = false;
  private final static boolean DEF_CHAT_HIDDEN = false;

  private final IPersistence pers;
  private final Map<UUID, PreferencesModel> cache;

  public PreferencesHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
    this.cache = new HashMap<>();
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  @Override
  public boolean isScoreboardHidden(Player p) {
    return getOrCreatePreferences(p)
      .map(PreferencesModel::isScoreboardHidden)
      .orElse(DEF_SCOREBOARD_HIDDEN);
  }

  @Override
  public void setScoreboardHidden(Player p, boolean hidden) {
    getOrCreatePreferences(p)
      .ifPresent(prefs -> {
        prefs.setScoreboardHidden(hidden);
        pers.store(prefs);
      });
  }

  @Override
  public boolean isChatHidden(Player p) {
    return getOrCreatePreferences(p)
      .map(PreferencesModel::isChatHidden)
      .orElse(DEF_CHAT_HIDDEN);
  }

  @Override
  public void setChatHidden(Player p, boolean hidden) {
    getOrCreatePreferences(p)
      .ifPresent(prefs -> {
        prefs.setChatHidden(hidden);
        pers.store(prefs);
      });
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Get the latest value of a player's preferences model (with an underlying cache)
   * @param p Target player
   * @return Preferences model
   */
  private Optional<PreferencesModel> getPreferences(Player p) {
    if (cache.containsKey(p.getUniqueId()))
      return Optional.of(cache.get(p.getUniqueId()));

    Optional<PreferencesModel> prefs = pers.findFirst(new QueryBuilder<>(
      PreferencesModel.class,
      "owner__uuid", EqualityOperation.EQ, p.getUniqueId()
    ));

    prefs.ifPresent(preferencesModel -> cache.put(p.getUniqueId(), preferencesModel));
    return prefs;
  }

  /**
   * Get the latest value of a player's preferences model or create a new
   * default entry before returning that
   * @param p Target player
   * @return Preferences model
   */
  private Optional<PreferencesModel> getOrCreatePreferences(Player p) {
    Optional<PreferencesModel> prefs = getPreferences(p);

    if (prefs.isEmpty()) {
      createDefault(p);
      return getPreferences(p);
    }

    return prefs;
  }

  /**
   * Create a new preferences entry with default values
   * if no entry yet exists
   * @param p Target player to create for
   */
  private void createDefault(Player p) {
    // Preferences entry already exists
    if (pers.count(new QueryBuilder<>(
      PreferencesModel.class,
      "owner__uuid", EqualityOperation.EQ, p.getUniqueId()
    )) > 0)
      return;

    PreferencesModel prefs = new PreferencesModel(
      p,
      DEF_SCOREBOARD_HIDDEN,
      DEF_CHAT_HIDDEN
    );
    pers.store(prefs);
  }
}
