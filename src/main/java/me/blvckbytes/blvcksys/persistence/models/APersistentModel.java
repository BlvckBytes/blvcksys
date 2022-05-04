package me.blvckbytes.blvcksys.persistence.models;

import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.ModelIdentifier;
import me.blvckbytes.blvcksys.persistence.ModelProperty;

import java.util.Date;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  The base of all models which hoists up common fields.
*/
public abstract class APersistentModel {

  @Getter
  @ModelIdentifier
  private UUID id;

  @Getter
  @ModelProperty(isInlineable = false)
  private Date createdAt;

  @Getter
  @ModelProperty(isInlineable = false)
  private Date updatedAt;

}
