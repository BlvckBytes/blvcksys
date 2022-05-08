package me.blvckbytes.blvcksys.persistence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.mysql.MysqlType;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Specifies the default value of a property used when migrating
  (extending) persistent data-structures.
*/
@Getter
@AllArgsConstructor
public enum MigrationDefault {
  NULL(
    null,
    new String[] {
      "NULL"
    },
    null
  ),

  TRUE(
    true,
    new String[] {
      "TRUE",
      "1"
    },
    new MysqlType[] {
      MysqlType.BOOLEAN
    }
  ),

  FALSE(
    false,
    new String[] {
      "FALSE",
      "0"
    },
    new MysqlType[] {
      MysqlType.BOOLEAN
    }
  ),

  ZERO(
    0,
    new String[] {
      "0"
    },
    new MysqlType[] {
      MysqlType.INTEGER,
      MysqlType.LONG,
      MysqlType.FLOAT
    }
  ),

  UNSPECIFIED(
    null,
    new String[] {},
    new MysqlType[] {}
  );

  private final Object javaValue;
  private final String[] sqlValues;
  private final MysqlType[] sqlTypes;

  /**
   * Check if this default's SQL type matches a given SQL type
   * @param type Type to check against
   * @return True on match, false otherwise
   */
  public boolean matchesSqlType(MysqlType type) {
    for (MysqlType ownType : sqlTypes) {
      if (ownType == type || this == NULL)
        return true;
    }
    return false;
  }

  /**
   * Check if this default's SQL value matches a given SQL value string
   * @param sqlValue Value string to check against
   * @return True on match, false otherwise
   */
  public boolean matchesSqlValue(String sqlValue) {
    for (String sv : sqlValues) {
      if (sv.equalsIgnoreCase(sqlValue))
        return true;
    }

    return false;
  }

  @Override
  public String toString() {
    return sqlValues[0];
  }
}
