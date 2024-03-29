package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.events.IChatListener;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.MuteModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldQueryGroup;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Manages creating, updating and deleting mutes.
*/
@AutoConstruct
public class MuteHandler implements IMuteHandler, Listener {

  private final Map<OfflinePlayer, MuteModel> cache;
  private final Set<OfflinePlayer> cached;

  private final IPersistence pers;
  private final IChatListener chat;
  private final IConfig cfg;
  private final TimeUtil time;

  public MuteHandler(
    @AutoInject IPersistence pers,
    @AutoInject IChatListener chat,
    @AutoInject IConfig cfg,
    @AutoInject TimeUtil time
  ) {
    this.cache = new HashMap<>();
    this.cached = new HashSet<>();

    this.pers = pers;
    this.chat = chat;
    this.cfg = cfg;
    this.time = time;
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  @Override
  public MuteModel createMute(
    OfflinePlayer creator,
    OfflinePlayer target,
    Integer durationSeconds,
    @Nullable String reason
  ) throws PersistenceException {
    MuteModel mute = new MuteModel(creator, target, durationSeconds, reason);
    pers.store(mute);
    cache.put(mute.getTarget(), mute);
    cached.add(target);
    return mute;
  }

  @Override
  public void broadcastMute(MuteModel mute) {
    chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.MUTE_CASTED_BROADCAST)
        .withPrefixes()
        .withVariables(buildMuteVariables(mute))
        .asScalar()
    );
  }

  @Override
  public List<MuteModel> listMutes(
    OfflinePlayer target,
    @Nullable Boolean isRevoked,
    @Nullable Boolean isActive
  ) throws PersistenceException {
    return pers.find(buildQuery(target, isRevoked, isActive));
  }

  @Override
  public Optional<MuteModel> isCurrentlyMuted(OfflinePlayer target) {
    if (!cached.contains(target)) {
      cached.add(target);

      return pers.findFirst(buildQuery(target, false, true))
        .map(model -> {
          cache.put(target, model);
          return model;
        });
    }

    MuteModel mute = cache.get(target);

    if (mute != null) {
      if (mute.isRevoked() || !mute.isActive()) {
        cache.remove(target);
        return Optional.empty();
      }

      return Optional.of(mute);
    }

    return Optional.empty();
  }

  @Override
  public Map<String, String> buildMuteVariables(MuteModel mute) {
    return ConfigValue.makeEmpty()
      .withVariable("creator", mute.getCreator().getName())
      .withVariable("target", mute.getTarget().getName())
      .withVariable("created_at", mute.getCreatedAtStr())
      .withVariable("duration", time.formatDuration(mute.getDurationSeconds()))
      .withVariable("remaining", time.formatDuration(mute.getRemainingSeconds()))
      .withVariable(
        "reason",
        mute.getReason() == null ?
          cfg.get(ConfigKey.MUTE_NO_REASON).asScalar() :
          mute.getReason()
      )
      .withVariable(
        "revocation_reason",
        mute.getRevocationReason() == null ?
          cfg.get(ConfigKey.MUTE_NO_REASON).asScalar() :
          mute.getRevocationReason()
      )
      .withVariable("id", mute.getId())
      .withVariable(
        "revoker",
        !mute.isRevoked() ?
          cfg.get(ConfigKey.MUTE_NO_REVOKED).asScalar() :
          mute.getRevoker().getName()
      )
      .withVariable(
        "revoked_at",
        mute.getRevokedAt() == null ?
          cfg.get(ConfigKey.MUTE_NO_REVOKED).asScalar() :
          mute.getRevokedAtStr()
      )
      .exportVariables();
  }

  @Override
  public Optional<MuteModel> findById(UUID id) {
    return pers.findFirst(
      new QueryBuilder<>(
        MuteModel.class,
        "id", EqualityOperation.EQ, id
      )
        .orderBy("updatedAt", false)
        .orderBy("createdAt", false)
    );
  }

  @Override
  public MuteModel revokeMute(MuteModel mute, Player revoker, @Nullable String reason) {
    if (mute.isRevoked())
      return null;

    mute.setRevoked(revoker, reason);
    pers.store(mute);
    cache.remove(mute.getTarget());
    cached.add(mute.getTarget());

    return mute;
  }

  @Override
  public void broadcastRevoke(MuteModel mute) {
    chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.MUTE_REVOKED_BROADCAST)
        .withPrefixes()
        .withVariables(buildMuteVariables(mute))
        .asScalar()
    );
  }

  @Override
  public String buildMuteScreen(MuteModel mute) {
    return cfg.get(ConfigKey.MUTE_SCREEN)
      .withVariables(buildMuteVariables(mute))
      .asScalar();
  }

  @Override
  public TriResult deleteMute(MuteModel mute) {
    if (mute.isActive())
      return TriResult.ERR;

    cache.remove(mute.getTarget());
    cached.add(mute.getTarget());

    if (!pers.delete(mute))
      return TriResult.EMPTY;

    return TriResult.SUCC;
  }

  //=========================================================================//
  //                                 Listener                                //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    cache.remove(e.getPlayer());
    cached.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Build a query based on a set of constraints
   * @param target Target player
   * @param isRevoked Whether the mute should have been revoked, null means doesn't matter
   * @param isActive Whether the mute should be currently active, null means doesn't matter
   * @return Mute query that will only return results which meet the constraints
   */
  private QueryBuilder<MuteModel> buildQuery(
    OfflinePlayer target,
    @Nullable Boolean isRevoked,
    @Nullable Boolean isActive
  ) {
    FieldQueryGroup targetQuery = new FieldQueryGroup(
      "target__uuid",
      EqualityOperation.EQ, target.getUniqueId()
    );

    QueryBuilder<MuteModel> query = new QueryBuilder<>(MuteModel.class, targetQuery);

    if (isRevoked != null)
      query.and("revokedAt", isRevoked ? EqualityOperation.NEQ : EqualityOperation.EQ, null);

    if (isActive != null) {
      query.and(
        new FieldQueryGroup(
          "createdAt", FieldOperation.PLUS, "durationSeconds",
          isActive ? EqualityOperation.GTE : EqualityOperation.LT,
          System.currentTimeMillis() / 1000
        )
      );
    }

    query.orderBy("updatedAt", false);
    query.orderBy("createdAt", false);

    return query;
  }
}
