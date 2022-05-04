package me.blvckbytes.blvcksys.persistence.mysql;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/05/2022

  Represents a column in a MySQL database.
*/
public record MysqlColumn(
  String name,
  MysqlType type,
  boolean isNullable,
  boolean isPrimaryKey,
  boolean isUnique,
  boolean isInlineable
) {}
