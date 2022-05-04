package me.blvckbytes.blvcksys.persistence.mysql;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/05/2022

  Represents a table in a MySQL database.
*/
public record MysqlTable(
  String name,
  List<MysqlColumn> columns,
  boolean isTransformer
) {}
