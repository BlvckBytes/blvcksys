package me.blvckbytes.blvcksys.persistence.mysql;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.transformers.IDataTransformer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/05/2022

  Represents a column in a MySQL database.
*/
@Getter
@AllArgsConstructor
public class MysqlColumn {
  private final String name;
  private final MysqlType type;
  private final boolean isNullable;
  private final boolean isUnique;

  // Whether this column can be inlined when using transformers
  private final boolean isInlineable;

  @Nullable private final MysqlColumn transformerColumn;

  // The corresponding field within the persistent model
  private final Field modelField;

  // The "id" field is reserved for the primary key
  public boolean isPrimaryKey() {
    return name.equals("id");
  }
}
