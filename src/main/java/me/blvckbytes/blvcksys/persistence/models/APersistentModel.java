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
@Getter
public abstract class APersistentModel {

  protected static final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

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
    return createdAt == null ? "/" : df.format(createdAt);
  }

  /**
   * Get the updatedAt timestamp as a human readable string
   */
  public String getUpdatedAtStr() {
    return updatedAt == null ? "/" : df.format(updatedAt);
  }
}
