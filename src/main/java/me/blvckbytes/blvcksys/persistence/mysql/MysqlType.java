package me.blvckbytes.blvcksys.persistence.mysql;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;

import java.util.Date;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/05/2022

  Maps a data-type in a MySQL database to it's corresponding Java type and provides
  flags like whether the type is of fixed length and what operations it supports.
*/
@AllArgsConstructor
public enum MysqlType {
  UUID(
    "BINARY(16)", java.util.UUID.class, true,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ
    }
  ),

  TEXT(
    "TEXT", String.class, false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.EQ_IC,
      EqualityOperation.NEQ_IC
    }
  ),

  VARCHAR(
    "VARCHAR(255)", String.class, true,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.EQ_IC,
      EqualityOperation.NEQ_IC
    }
  ),

  BOOLEAN(
    "BOOL", boolean.class, false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ
    }
  ),

  INTEGER(
    "INT", int.class, false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.GT,
      EqualityOperation.GTE,
      EqualityOperation.LT,
      EqualityOperation.LTE
    }
  ),

  DOUBLE(
    "DOUBLE", double.class, false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.GT,
      EqualityOperation.GTE,
      EqualityOperation.LT,
      EqualityOperation.LTE
    }
  ),

  FLOAT(
    "FLOAT", float.class, false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.GT,
      EqualityOperation.GTE,
      EqualityOperation.LT,
      EqualityOperation.LTE
    }
  ),

  DATETIME(
    "DATETIME", Date.class, false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.GT,
      EqualityOperation.GTE,
      EqualityOperation.LT,
      EqualityOperation.LTE
    }
  )
  ;

  private final String queryType;
  private final Class<?> javaEquivalent;
  private final boolean hasLength;
  private final EqualityOperation[] supportedOps;

  @Override
  public String toString() {
    return queryType;
  }

  /**
   * Converts a java type to a MySQL type
   * @param javaType Java type to convert
   * @param lengthRequired Whether this type has to have a specified length
   * @return Optional MySQL type
   */
  public static Optional<MysqlType> fromJavaType(Class<?> javaType, boolean lengthRequired) {
    for (MysqlType type : MysqlType.values()) {
      if (type.javaEquivalent.equals(javaType) && (!lengthRequired || type.hasLength))
        return Optional.of(type);
    }
    return Optional.empty();
  }

  /**
   * Check if this type supports a given equality operation
   * @param op Equality operation to check
   */
  public boolean supportsOp(EqualityOperation op) {
    for (EqualityOperation supOp : supportedOps) {
      if (supOp == op)
        return true;
    }
    return false;
  }
}
