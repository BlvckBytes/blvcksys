package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Represents a model that has a cooldown to it's use.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ACooldownModel extends APersistentModel {

  @ModelProperty
  protected int cooldownSeconds;

  /**
   * Generate a token which will be unique for this model instance's properties
   */
  protected String generateToken() {
    Class<?> c = getClass();
    StringBuilder token = new StringBuilder(c.getSimpleName().toLowerCase());

    try {

      // Loop the class hierarchy to find all declared fields
      boolean uniqueFieldFound = false;
      while (c.getSuperclass() != null) {
        Field[] fields = c.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
          Field f = fields[i];

          // Not a model property or not unique
          ModelProperty mp = f.getAnnotation(ModelProperty.class);
          if (mp == null || !mp.isUnique())
            continue;

          f.setAccessible(true);

          // Append the unique field's value to the token
          token
            .append(i == 0 ? "__" : "_")
            .append(f.get(this));

          uniqueFieldFound = true;
        }
        c = c.getSuperclass();
      }

      if (!uniqueFieldFound)
        throw new IllegalStateException("Model contains no unique fields");

    } catch (Exception e) {
      throw new PersistenceException("Could not generate a unique token for " + c + ": " + e.getMessage());
    }

    return token.toString();
  }

  /**
   * Create a new cooldown for a given player which will be targetting
   * the current model's unique properties and store it.
   * @param holder Cooldown holder
   * @param pers Persistency ref
   */
  public void storeCooldownFor(OfflinePlayer holder, IPersistence pers) throws PersistenceException {
    pers.store(new CooldownSessionModel(
      holder,
      new Date(System.currentTimeMillis() + cooldownSeconds * 1000L),
      generateToken()
    ));
  }

  /**
   * Get the remaining cooldown duration in seconds for a given player
   * which is targetting the current model's unique properties. If the cooldown
   * is already expired, this call will delete the unneeded entry.
   * @param holder Cooldown holder
   * @param pers Persistency ref
   * @return -1 if there's no active cooldown or the number of seconds remaining
   */
  public long getCooldownRemaining(OfflinePlayer holder, IPersistence pers) throws PersistenceException {
    // Get all cooldowns from this player that match the current token
    List<CooldownSessionModel> cooldowns = pers.find(
      new QueryBuilder<>(
        CooldownSessionModel.class,
        "holder__uuid", EqualityOperation.EQ, holder.getUniqueId()
      ).and("token", EqualityOperation.EQ, generateToken())
    );

    // This player has no cooldowns
    if (cooldowns.isEmpty())
      return -1;

    for (CooldownSessionModel cooldown : cooldowns) {
      // Calculate the remaining time in seconds
      long remaining = (cooldown.getExpiresAt().getTime() - System.currentTimeMillis()) / 1000;

      // Delete dead entries
      if (remaining <= 0) {
        pers.delete(cooldown);
        continue;
      }

      return remaining;
    }

    // Only had dead entries
    return -1;
  }
}
