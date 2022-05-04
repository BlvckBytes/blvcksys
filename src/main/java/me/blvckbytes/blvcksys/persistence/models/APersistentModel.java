package me.blvckbytes.blvcksys.persistence.models;

import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.ModelProperty;

import java.util.Date;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  The base of all models which adds common functionality.
*/
public abstract class APersistentModel<T> {

  @Getter
  @ModelProperty
  private Date createdAt;

  @Getter
  @ModelProperty
  private Date updatedAt;
}
