package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.events.IChatListener;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.BanModel;
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
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.regex.Pattern;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/13/2022

  Manages creating, updating and deleting bans as well as checking
  login attempts and making sure to cancel disallowed players.
*/
@AutoConstruct
public class BanHandler implements IBanHandler, Listener {

  private final IPersistence pers;
  private final IChatListener chat;
  private final IConfig cfg;
  private final TimeUtil time;

  public BanHandler(
    @AutoInject IPersistence pers,
    @AutoInject IChatListener chat,
    @AutoInject IConfig cfg,
    @AutoInject TimeUtil time
  ) {
    this.pers = pers;
    this.chat = chat;
    this.cfg = cfg;
    this.time = time;
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  @Override
  public BanModel createBan(
    OfflinePlayer creator,
    OfflinePlayer target,
    @Nullable Integer durationSeconds,
    @Nullable String ipAddress,
    @Nullable String reason
  ) throws PersistenceException {
    BanModel ban = new BanModel(creator, target, durationSeconds, ipAddress, reason, null, null, null);
    pers.store(ban);

    String banScreen = buildBanScreen(ban);

    // Kick the player if they're online
    if (target.isOnline())
      ((Player) target).kickPlayer(banScreen);

    // Kick all other players running on the same address
    if (ipAddress != null) {
      for (Player t : Bukkit.getOnlinePlayers()) {
        InetSocketAddress addr = t.getAddress();

        if (addr != null && addr.getAddress().getHostAddress().equals(ipAddress))
          t.kickPlayer(banScreen);
      }
    }

    return ban;
  }

  @Override
  public void broadcastBan(BanModel ban) {
    chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.BAN_CASTED_BROADCAST)
        .withPrefixes()
        .withVariables(buildBanVariables(ban))
        .asScalar()
    );
  }

  @Override
  public List<BanModel> listBans(
    OfflinePlayer target,
    @Nullable Boolean isPermanent,
    @Nullable Boolean hasIpAddress,
    @Nullable Boolean isRevoked,
    @Nullable Boolean isActive
  ) throws PersistenceException {
    return pers.find(buildQuery(target, null, isPermanent, hasIpAddress, isRevoked, isActive));
  }

  @Override
  public Optional<BanModel> isCurrentlyBanned(OfflinePlayer target, @Nullable InetAddress addr) {
    // Non-revoked, still active (if temporary) bans
    return pers.find(buildQuery(target, addr, null, null, false, true))
      .stream()

      // Sort by is permanent, then by more recent
      .min((a, b) -> {
        if (a.getDurationSeconds() == null && b.getDurationSeconds() != null)
          return 1;

        if (a.getDurationSeconds() != null && b.getDurationSeconds() == null)
          return -1;

        return a.getCreatedAt().compareTo(b.getCreatedAt());
      });
  }

  @Override
  public Map<Pattern, String> buildBanVariables(BanModel ban) {
    int remaining = 0;

    // Remaining: (createdAt + durationSections) - now
    if (ban.getDurationSeconds() != null) {
      remaining = (int) Math.max(0,
        (ban.getCreatedAt().getTime() / 1000 + ban.getDurationSeconds()) -
          (System.currentTimeMillis() / 1000)
      );
    }

    return ConfigValue.makeEmpty()
      .withVariable("creator", ban.getCreator().getName())
      .withVariable("target", ban.getTarget().getName())
      .withVariable(
        "duration",
        ban.getDurationSeconds() == null ?
          cfg.get(ConfigKey.BAN_DURATION_PERMANENT).asScalar() :
          time.formatDuration(ban.getDurationSeconds())
      )
      .withVariable(
        "remaining",
        ban.getDurationSeconds() == null ?
          cfg.get(ConfigKey.BAN_REMAINING_PERMANENT).asScalar() :
          time.formatDuration(remaining)
      )
      .withVariable(
        "reason",
        ban.getReason() == null ?
          cfg.get(ConfigKey.BAN_NO_REASON).asScalar() :
          ban.getReason()
      )
      .withVariable(
        "revocation_reason",
        ban.getRevocationReason() == null ?
          cfg.get(ConfigKey.BAN_NO_REASON).asScalar() :
          ban.getRevocationReason()
      )
      .withVariable(
        "ip",
        ban.getIpAddress() == null ?
          cfg.get(ConfigKey.BAN_NO_ADDRESS).asScalar() :
          ban.getIpAddress()
      )
      .withVariable("id", ban.getId())
      .withVariable("target", ban.getTarget().getName())
      .withVariable(
        "revoker",
        ban.getRevoker() == null ?
          cfg.get(ConfigKey.BAN_NO_REVOKED).asScalar() :
          ban.getRevoker().getName()
      )
      .withVariable(
        "revoked_at",
        ban.getRevokedAt() == null ?
          cfg.get(ConfigKey.BAN_NO_REVOKED).asScalar() :
          ban.getRevokedAtStr()
      )
      .exportVariables();
  }

  @Override
  public Optional<BanModel> findById(UUID id) {
    return pers.findFirst(
      new QueryBuilder<>(
        BanModel.class,
        "id", EqualityOperation.EQ, id
      )
    );
  }

  @Override
  public BanModel revokeBan(BanModel ban, Player revoker, @Nullable String reason) {
    if (ban.getRevoker() != null)
      return null;

    ban.setRevokedAt(new Date());
    ban.setRevoker(revoker);
    ban.setRevocationReason(reason);
    pers.store(ban);

    return ban;
  }

  @Override
  public void broadcastRevoke(BanModel ban) {
    chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.BAN_REVOKED_BROADCAST)
        .withPrefixes()
        .withVariables(buildBanVariables(ban))
        .asScalar()
    );
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onLogin(PlayerLoginEvent e) {
    Optional<BanModel> ban = isCurrentlyBanned(e.getPlayer(), e.getAddress());

    if (ban.isEmpty())
      return;

    e.setResult(PlayerLoginEvent.Result.KICK_BANNED);
    e.setKickMessage(buildBanScreen(ban.get()));
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Build a query based on a set of constraints
   * @param target Target player
   * @param addr Address to check against as well
   * @param isPermanent Whether bans should be permanent, null means doesn't matter
   * @param hasIpAddress Whether an address should be stored with the ban, null means doesn't matter
   * @param isRevoked Whether the ban should have been revoked, null means doesn't matter
   * @param isActive Whether the ban should be currently active, null means doesn't matter
   * @return Ban query that will only return results which meet the constraints
   */
  private QueryBuilder<BanModel> buildQuery(
    OfflinePlayer target,
    @Nullable InetAddress addr,
    @Nullable Boolean isPermanent,
    @Nullable Boolean hasIpAddress,
    @Nullable Boolean isRevoked,
    @Nullable Boolean isActive
  ) {
    FieldQueryGroup targetQuery = new FieldQueryGroup(
      "target__uuid",
      EqualityOperation.EQ, target.getUniqueId()
    );

    if (addr != null)
      targetQuery.or("ipAddress", EqualityOperation.EQ_IC, addr.getHostAddress());

    QueryBuilder<BanModel> query = new QueryBuilder<>(BanModel.class, targetQuery);

    if (isPermanent != null)
      query.and("durationSeconds", isPermanent ? EqualityOperation.NEQ : EqualityOperation.EQ, null);

    if (hasIpAddress != null)
      query.and("ipAddress", hasIpAddress ? EqualityOperation.NEQ : EqualityOperation.EQ, null);

    if (isRevoked != null)
      query.and("revokedAt", isRevoked ? EqualityOperation.NEQ : EqualityOperation.EQ, null);

    if (isActive != null) {
      query.and(
        // Permanent ban
        new FieldQueryGroup(
          "durationSeconds", EqualityOperation.EQ, null
        )
          // Still active temporary ban
          .or(
            "createdAt", FieldOperation.PLUS, "durationSeconds",
            isActive ? EqualityOperation.GTE : EqualityOperation.LT,
            System.currentTimeMillis() / 1000
          )
      );
    }

    return query;
  }

  /**
   * Build a ban screen from the parameters of an active ban
   * @param ban Active ban
   * @return Formatted and customized ban screen
   */
  private String buildBanScreen(BanModel ban) {
    return cfg.get(ConfigKey.BAN_SCREEN)
      .withVariables(buildBanVariables(ban))
      .asScalar();
  }
}
