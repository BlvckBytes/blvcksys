package me.blvckbytes.blvcksys.persistence.models;

import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  The base of all models which hoists up common fields.
*/
@Getter
public abstract class APersistentModel {

  protected static final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
  protected static final SimpleDateFormat dfShort = new SimpleDateFormat("dd.MM.yyyy");

  @ModelProperty
  protected UUID id;

  @ModelProperty(isInlineable = false)
  protected Date createdAt;

  @ModelProperty(isInlineable = false, isNullable = true)
  protected Date updatedAt;

  /**
   * Get the createdAt timestamp as a human readable string
   */
  public String getCreatedAtStr() {
    return getCreatedAtStr(false);
  }

  /**
   * Get the createdAt timestamp as a human readable string
   * @param shortFormat Whether to display only the date without the time
   */
  public String getCreatedAtStr(boolean shortFormat) {
    return createdAt == null ? "/" : (shortFormat ? dfShort : df).format(createdAt);
  }

  /**
   * Get the updatedAt timestamp as a human readable string
   */
  public String getUpdatedAtStr() {
    return getUpdatedAtStr(false);
  }

  /**
   * Get the updatedAt timestamp as a human readable string
   * @param shortFormat Whether to display only the date without the time
   */
  public String getUpdatedAtStr(boolean shortFormat) {
    return updatedAt == null ? "/" : (shortFormat ? dfShort : df).format(updatedAt);
  }

  /**
   * Compares two players for equality
   * @param a Player A
   * @param b Player B
   * @return Equality state
   */
  public boolean comparePlayers(OfflinePlayer a, OfflinePlayer b) {
    return a.getUniqueId().equals(b.getUniqueId());
  }
}
