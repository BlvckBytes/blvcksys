package me.blvckbytes.blvcksys.persistence.mysql;

import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/05/2022

  Maps a data-type in a MySQL database to it's corresponding Java type.
*/
@AllArgsConstructor
public enum MysqlType {
  UUID("BINARY(16)", java.util.UUID.class, true),
  TEXT("TEXT", String.class, false),
  VARCHAR("VARCHAR(255)", String.class, true),
  BOOLEAN("BOOL", boolean.class, false),
  INTEGER("INT", int.class, false),
  DOUBLE("DOUBLE", double.class, false),
  FLOAT("FLOAT", float.class, false),
  DATETIME("DATETIME", Date.class, false)
  ;

  private final String queryType;
  private final Class<?> javaEquivalent;
  private final boolean hasLength;

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
}
