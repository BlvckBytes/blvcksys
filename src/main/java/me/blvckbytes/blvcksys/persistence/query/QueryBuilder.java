package me.blvckbytes.blvcksys.persistence.query;

import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.models.APersistentModel;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  5reated On: 05/06/2022

  Holds groups of field queries, connected logically, as well as extra parameters.
*/
@Getter
public class QueryBuilder<T extends APersistentModel> {

  private final Class<T> model;
  private final FieldQueryGroup root;
  private final List<Tuple<QueryConnection, FieldQueryGroup>> additionals;

  private Integer limit = null;
  private Integer skip = null;

  /**
   * Create a new query, starting of with an initial field query
   * @param field Name of the field
   * @param op Equality operation
   * @param value Target value of the operation
   */
  public QueryBuilder(Class<T> model, String field, EqualityOperation op, Object value) {
    this(model, new FieldQueryGroup(field, op, value));
  }

  /**
   * Create a new query, starting of with an initial query group
   * @param group Field query group
   */
  public QueryBuilder(Class<T> model, FieldQueryGroup group) {
    this.additionals = new ArrayList<>();
    this.model = model;
    this.root = group;
  }

  /**
   * Add a new field query to the last query, connected with a logical AND
   * @param field Name of the field
   * @param op Equality operation
   * @param value Target value of the operation
   */
  public QueryBuilder<T> and(String field, EqualityOperation op, Object value) {
    additionals.add(new Tuple<>(QueryConnection.AND, new FieldQueryGroup(field, op, value)));
    return this;
  }

  /**
   * Add a new field query group to the last query, connected with a logical AND
   * @param group Field query group
   */
  public QueryBuilder<T> and(FieldQueryGroup group) {
    additionals.add(new Tuple<>(QueryConnection.AND, group));
    return this;
  }

  /**
   * Add a new field query to the last query, connected with a logical OR
   * @param field Name of the field
   * @param op Equality operation
   * @param value Target value of the operation
   */
  public QueryBuilder<T> or(String field, EqualityOperation op, Object value) {
    additionals.add(new Tuple<>(QueryConnection.OR, new FieldQueryGroup(field, op, value)));
    return this;
  }

  /**
   * Add a new field query group to the last query, connected with a logical OR
   * @param group Field query group
   */
  public QueryBuilder<T> or(FieldQueryGroup group) {
    additionals.add(new Tuple<>(QueryConnection.OR, group));
    return this;
  }

  /**
   * Skip a specified number of results
   * @param numResults Number of results to skip
   */
  public QueryBuilder<T> skip(int numResults) {
    if (numResults < 0)
      throw new IllegalArgumentException("Cannot skip a negative amount of results: " + numResults);

    this.skip = numResults;
    return this;
  }

  /**
   * Limit the number of results to a specified amount
   * @param maxResults Number of results to be limiting to
   */
  public QueryBuilder<T> limit(int maxResults) {
    if (maxResults < 0)
      throw new IllegalArgumentException("Cannot limit to a negative amount of results: " + maxResults);

    this.limit = maxResults;
    return this;
  }
}
