package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.events.IChatListener;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.WarnModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldQueryGroup;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.TimeUtil;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Manages creating, updating and deleting warns as well as
  executing punishments when the maximum number of active
  warns has been reached by a player.
*/
@AutoConstruct
public class WarnHandler implements IWarnHandler {

  private static final int MAX_ACTIVE_WARNS = 10;

  private final IPersistence pers;
  private final IConfig cfg;
  private final IChatListener chat;
  private final TimeUtil time;
  private final IBanHandler bans;

  public WarnHandler(
    @AutoInject IPersistence pers,
    @AutoInject IConfig cfg,
    @AutoInject IChatListener chat,
    @AutoInject TimeUtil time,
    @AutoInject IBanHandler bans
  ) {
    this.pers = pers;
    this.cfg = cfg;
    this.chat = chat;
    this.time = time;
    this.bans = bans;
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  @Override
  public Optional<WarnModel> createWarn(
    OfflinePlayer creator,
    OfflinePlayer target,
    Integer durationSeconds,
    @Nullable String reason
  ) throws PersistenceException {
    int hasCount = countActiveWarns(target);
    int maxWarns = getMaxActiveWarns();

    if (hasCount >= maxWarns)
      return Optional.empty();

    WarnModel warn = new WarnModel(creator, target, durationSeconds, reason, hasCount + 1);
    pers.store(warn);
    checkActiveWarns(creator, target);

    return Optional.of(warn);
  }

  @Override
  public void broadcastWarn(WarnModel warn) {
    chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.WARN_CASTED_BROADCAST)
        .withPrefixes()
        .withVariables(buildWarnVariables(warn))
        .asScalar()
    );
  }

  @Override
  public List<WarnModel> listWarns(
    OfflinePlayer target,
    @Nullable Boolean isPermanent,
    @Nullable Boolean isRevoked,
    @Nullable Boolean isActive
  ) throws PersistenceException {
    return pers.find(buildQuery(target, isPermanent, isRevoked, isActive));
  }

  @Override
  public Map<String, Tuple<Pattern, String>> buildWarnVariables(WarnModel warn) {
    int remaining = 0;

    // Remaining: (createdAt + durationSections) - now
    if (warn.getDurationSeconds() != null) {
      remaining = (int) Math.max(0,
        (warn.getCreatedAt().getTime() / 1000 + warn.getDurationSeconds()) -
          (System.currentTimeMillis() / 1000)
      );
    }

    return ConfigValue.makeEmpty()
      .withVariable("creator", warn.getCreator().getName())
      .withVariable("target", warn.getTarget().getName())
      .withVariable("created_at", warn.getCreatedAtStr())
      .withVariable("warn_number", warn.getNumber())
      .withVariable(
        "duration",
        warn.getDurationSeconds() == null ?
          cfg.get(ConfigKey.WARN_DURATION_PERMANENT).asScalar() :
          time.formatDuration(warn.getDurationSeconds())
      )
      .withVariable(
        "remaining",
        warn.getDurationSeconds() == null ?
          cfg.get(ConfigKey.WARN_REMAINING_PERMANENT).asScalar() :
          time.formatDuration(remaining)
      )
      .withVariable(
        "reason",
        warn.getReason() == null ?
          cfg.get(ConfigKey.WARN_NO_REASON).asScalar() :
          warn.getReason()
      )
      .withVariable(
        "revocation_reason",
        warn.getRevocationReason() == null ?
          cfg.get(ConfigKey.WARN_NO_REASON).asScalar() :
          warn.getRevocationReason()
      )
      .withVariable("id", warn.getId())
      .withVariable(
        "revoker",
        !warn.isRevoked() ?
          cfg.get(ConfigKey.WARN_NO_REVOKED).asScalar() :
          warn.getRevoker().getName()
      )
      .withVariable(
        "revoked_at",
        warn.getRevokedAt() == null ?
          cfg.get(ConfigKey.WARN_NO_REVOKED).asScalar() :
          warn.getRevokedAtStr()
      )
      .exportVariables();
  }

  @Override
  public Optional<WarnModel> findById(UUID id) {
    return pers.findFirst(
      new QueryBuilder<>(
        WarnModel.class,
        "id", EqualityOperation.EQ, id
      )
        .orderBy("createdAt", false)
        .orderBy("updatedAt", false)
    );
  }

  @Override
  public WarnModel revokeWarn(WarnModel warn, Player revoker, @Nullable String reason) {
    if (warn.isRevoked())
      return null;

    warn.setRevoked(revoker, reason);
    pers.store(warn);

    return warn;
  }

  @Override
  public void broadcastRevoke(WarnModel warn) {
    chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.WARN_REVOKED_BROADCAST)
        .withPrefixes()
        .withVariables(buildWarnVariables(warn))
        .asScalar()
    );
  }

  @Override
  public void broadcastClear(OfflinePlayer target, int numCleared) {
    chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.WARN_CLEARED_BROADCAST)
        .withPrefixes()
        .withVariable("target", target.getName())
        .withVariable("num_cleared", numCleared)
        .asScalar()
    );
  }

  @Override
  public void clearWarns(OfflinePlayer target) {
    pers.delete(buildQuery(target, null, null, null));
  }

  @Override
  public int countAllWarns(OfflinePlayer target) {
    return pers.count(buildQuery(target, null, null, null));
  }

  @Override
  public int countActiveWarns(OfflinePlayer target) {
    return pers.count(buildQuery(target, null, false, true));
  }

  @Override
  public int getMaxActiveWarns() {
    return MAX_ACTIVE_WARNS;
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Check the number of active warns a player has, and create
   * a punishment if the number of active warns exceeded the maximum.
   * @param executor Executor of the last added warn, will be the executor used for auto punishments
   * @param target Target to check
   */
  private void checkActiveWarns(OfflinePlayer executor, OfflinePlayer target) {
    int active = countActiveWarns(target);

    if (active < MAX_ACTIVE_WARNS)
      return;

    bans.broadcastBan(bans.createBan(
      executor, target, null, null,
      cfg.get(ConfigKey.WARN_AUTO_PUNISHMENT_REASON)
        .withVariable("max_warns", MAX_ACTIVE_WARNS)
        .asScalar()
    ));
  }

  /**
   * Build a query based on a set of constraints
   * @param target Target player
   * @param isPermanent Whether warns should be permanent, null means doesn't matter
   * @param isRevoked Whether the warn should have been revoked, null means doesn't matter
   * @param isActive Whether the warn should be currently active, null means doesn't matter
   * @return Warn query that will only return results which meet the constraints
   */
  private QueryBuilder<WarnModel> buildQuery(
    OfflinePlayer target,
    @Nullable Boolean isPermanent,
    @Nullable Boolean isRevoked,
    @Nullable Boolean isActive
  ) {
    FieldQueryGroup targetQuery = new FieldQueryGroup(
      "target__uuid",
      EqualityOperation.EQ, target.getUniqueId()
    );

    QueryBuilder<WarnModel> query = new QueryBuilder<>(WarnModel.class, targetQuery);

    if (isPermanent != null)
      query.and("durationSeconds", isPermanent ? EqualityOperation.EQ : EqualityOperation.NEQ, null);

    if (isRevoked != null)
      query.and("revokedAt", isRevoked ? EqualityOperation.NEQ : EqualityOperation.EQ, null);

    if (isActive != null) {
      query.and(
        // Permanent warn
        new FieldQueryGroup(
          "durationSeconds", EqualityOperation.EQ, null
        )
          // Still active temporary warn
          .or(
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
