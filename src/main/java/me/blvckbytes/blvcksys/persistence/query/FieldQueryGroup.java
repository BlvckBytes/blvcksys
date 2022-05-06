package me.blvckbytes.blvcksys.persistence.query;

import lombok.Getter;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  5reated On: 05/06/2022

  Represents a group of field queries, connected logically, indicating a level of precedence.
*/
public class FieldQueryGroup {

  @Getter
  private final List<Tuple<QueryConnection, FieldQuery>> additionals;

  @Getter
  private final FieldQuery root;

  /**
   * Create a new group of field queries, starting of with the initial query
   * @param field Name of the field
   * @param op Equality operation
   * @param value Target value of the operation
   */
  public FieldQueryGroup(String field, EqualityOperation op, Object value) {
    this.additionals = new ArrayList<>();
    this.root = new FieldQuery(field, op, value);
  }

  /**
   * Add a new field query to the last query, connected with a logical AND
   * @param field Name of the field
   * @param op Equality operation
   * @param value Target value of the operation
   */
  public FieldQueryGroup and(String field, EqualityOperation op, Object value) {
    this.additionals.add(new Tuple<>(QueryConnection.AND, new FieldQuery(field, op, value)));
    return this;
  }

  /**
   * Add a new field query to the last query, connected with a logical OR
   * @param field Name of the field
   * @param op Equality operation
   * @param value Target value of the operation
   */
  public FieldQueryGroup or(String field, EqualityOperation op, Object value) {
    this.additionals.add(new Tuple<>(QueryConnection.OR, new FieldQuery(field, op, value)));
    return this;
  }
}
