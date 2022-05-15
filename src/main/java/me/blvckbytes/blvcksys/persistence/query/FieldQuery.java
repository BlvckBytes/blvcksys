package me.blvckbytes.blvcksys.persistence.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  5reated On: 05/06/2022

  Conveniently wraps an equality query for a given field into a record.
  The field that is queried might be an operation result performed on two fields.
*/
@Getter
@AllArgsConstructor
public class FieldQuery {

  private final String fieldA;
  @Nullable private final FieldOperation fieldOp;
  @Nullable private final String fieldB;
  private final EqualityOperation eqOp;
  private final Object value;

  public FieldQuery(String field, EqualityOperation eqOp, Object value) {
    this(field, null, null, eqOp, value);
  }
}
