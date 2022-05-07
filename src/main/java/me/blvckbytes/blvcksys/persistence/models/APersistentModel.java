package me.blvckbytes.blvcksys.persistence.models;

import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.ModelProperty;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  The base of all models which hoists up common fields.
*/
public abstract class APersistentModel {

  private static final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  @Getter
  @ModelProperty
  private UUID id;

  @Getter
  @ModelProperty(isInlineable = false)
  private Date createdAt;

  @Getter
  @ModelProperty(isInlineable = false, isNullable = true)
  private Date updatedAt;

  /**
   * Get the createdAt timestamp as a human readable string
   */
  public String getCreatedAtStr() {
    return createdAt == null ? "/" : df.format(createdAt);
  }

  /**
   * Get the updatedAt timestamp as a human readable string
   */
  public String getUpdatedAtStr() {
    return updatedAt == null ? "/" : df.format(updatedAt);
  }
}
