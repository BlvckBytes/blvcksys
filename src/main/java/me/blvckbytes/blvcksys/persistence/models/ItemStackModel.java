package me.blvckbytes.blvcksys.persistence.models;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.blvckbytes.blvcksys.persistence.ModelProperty;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  An individual itemstack which has a base64 encoded string of it's properties.
*/
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemStackModel extends APersistentModel {

  @Getter
  @ModelProperty
  private String base64Item;
}
