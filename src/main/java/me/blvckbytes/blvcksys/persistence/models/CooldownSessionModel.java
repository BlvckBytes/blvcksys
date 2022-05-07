package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.OfflinePlayer;

import java.util.Date;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Saves a cooldown which has an expiry stamp and an identifying
  token for a holding player. Tokens represent a way of connecting the
  cooldown session to a resource and it's properties at which the
  cooldown is targetted at. See {@link ACooldownModel}
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CooldownSessionModel extends APersistentModel {

  public CooldownSessionModel(OfflinePlayer holder, int durationSeconds, String token) {
    this(holder, new Date(System.currentTimeMillis() + durationSeconds * 1000L), token);
  }

  @ModelProperty
  private OfflinePlayer holder;

  @ModelProperty
  private Date expiresAt;

  @ModelProperty
  private String token;

  /**
   * Get the remaining cooldown duration in seconds for a given player
   * which is targetting a specified token.
   * @param holder Cooldown holder
   * @param pers Persistency ref
   * @return -1 if there's no active cooldown or the number of seconds remaining
   */
  public static long getCooldownRemaining(
    OfflinePlayer holder,
    IPersistence pers,
    String token
  ) throws PersistenceException {
    // Get all cooldowns from this player that match the current token
    List<CooldownSessionModel> cooldowns = pers.find(
      new QueryBuilder<>(
        CooldownSessionModel.class,
        "holder__uuid", EqualityOperation.EQ, holder.getUniqueId()
      ).and("token", EqualityOperation.EQ, token)
    );

    // This player has no cooldowns
    if (cooldowns.isEmpty())
      return -1;

    long ret = -1;
    for (CooldownSessionModel cooldown : cooldowns) {
      // Calculate the remaining time in seconds
      long remaining = (cooldown.getExpiresAt().getTime() - System.currentTimeMillis()) / 1000;

      // Delete dead entries
      if (remaining <= 0) {
        pers.delete(cooldown);
        continue;
      }

      // Only set the return value initially or on
      // duplicate cooldowns that have a higher remaining
      // duration (just to support for cases of errors where
      // multiple entries got inserted, doesn't cost any extra)
      if (ret < 0 || remaining > ret)
        ret = remaining;
    }

    // Only had dead entries
    return ret;
  }
}