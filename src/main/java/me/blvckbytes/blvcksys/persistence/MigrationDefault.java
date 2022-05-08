package me.blvckbytes.blvcksys.persistence;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Specifies the default value of a property used when migrating
  (extending) persistent data-structures.
*/
@Getter
@AllArgsConstructor
public enum MigrationDefault {
  NULL(null),
  TRUE(true),
  FALSE(false),
  ZERO(0)
  ;

  private final Object value;
}
