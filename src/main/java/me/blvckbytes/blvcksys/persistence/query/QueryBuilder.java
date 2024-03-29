package me.blvckbytes.blvcksys.persistence.query;

import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.models.APersistentModel;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  5reated On: 05/06/2022

  Holds groups of field queries, connected logically, as well as extra parameters.
*/
@Getter
public class QueryBuilder<T extends APersistentModel> {

  private final Class<T> model;
  private @Nullable FieldQueryGroup root;
  private final List<Tuple<QueryConnection, FieldQueryGroup>> additionals;
  private final Map<String, Boolean> sorting;

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
   * Create a new query, starting of with an initial field query
   * @param fieldA Field A of the field operation
   * @param fOp Operation between field A and B
   * @param fieldB Field B of the field operation
   * @param eqOp Equality operation
   * @param value Target value of the operation
   */
  public QueryBuilder(Class<T> model, String fieldA, FieldOperation fOp, String fieldB, EqualityOperation eqOp, Object value) {
    this(model, new FieldQueryGroup(fieldA, fOp, fieldB, eqOp, value));
  }

  /**
   * Create a new query, starting of with an initial query group
   * @param group Field query group
   */
  public QueryBuilder(Class<T> model, @Nullable FieldQueryGroup group) {
    this.additionals = new ArrayList<>();
    this.sorting = new LinkedHashMap<>();
    this.model = model;
    this.root = group;
  }

  /**
   * Create a new query, without an initial query group
   */
  public QueryBuilder(Class<T> model) {
    this(model, null);
  }

  /**
   * Add a new field query to the last query, connected with a logical AND
   * @param field Name of the field
   * @param op Equality operation
   * @param value Target value of the operation
   */
  public QueryBuilder<T> and(String field, EqualityOperation op, Object value) {
    addOrUseAsRoot(new FieldQueryGroup(field, op, value), QueryConnection.AND);
    return this;
  }

  /**
   * Add a new field query to the last query, connected with a logical AND
   * @param fieldA Field A of the field operation
   * @param fOp Operation between field A and B
   * @param fieldB Field B of the field operation
   * @param eqOp Equality operation
   * @param value Target value of the operation
   */
  public QueryBuilder<T> and(String fieldA, FieldOperation fOp, String fieldB, EqualityOperation eqOp, Object value) {
    addOrUseAsRoot(new FieldQueryGroup(fieldA, fOp, fieldB, eqOp, value), QueryConnection.AND);
    return this;
  }

  /**
   * Add a new field query group to the last query, connected with a logical AND
   * @param group Field query group
   */
  public QueryBuilder<T> and(FieldQueryGroup group) {
    addOrUseAsRoot(group, QueryConnection.AND);
    return this;
  }

  /**
   * Add a new field query to the last query, connected with a logical OR
   * @param field Name of the field
   * @param op Equality operation
   * @param value Target value of the operation
   */
  public QueryBuilder<T> or(String field, EqualityOperation op, Object value) {
    addOrUseAsRoot(new FieldQueryGroup(field, op, value), QueryConnection.OR);
    return this;
  }

  /**
   * Add a new field query to the last query, connected with a logical OR
   * @param fieldA Field A of the field operation
   * @param fOp Operation between field A and B
   * @param fieldB Field B of the field operation
   * @param eqOp Equality operation
   * @param value Target value of the operation
   */
  public QueryBuilder<T> or(String fieldA, FieldOperation fOp, String fieldB, EqualityOperation eqOp, Object value) {
    addOrUseAsRoot(new FieldQueryGroup(fieldA, fOp, fieldB, eqOp, value), QueryConnection.OR);
    return this;
  }

  /**
   * Add a new field query group to the last query, connected with a logical OR
   * @param group Field query group
   */
  public QueryBuilder<T> or(FieldQueryGroup group) {
    addOrUseAsRoot(group, QueryConnection.OR);
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

  /**
   * Order the results by a specific field
   * @param field Field to order by
   * @param ascending Whether to sort in ascending order (true) or descending order (false)
   */
  public QueryBuilder<T> orderBy(String field, boolean ascending) {
    this.sorting.put(field, ascending);
    return this;
  }

  /**
   * Add another field query group to the list of additional groups or
   * use it as root, if root is still null
   * @param group Group to add
   * @param conn Connection to add it with
   */
  private void addOrUseAsRoot(FieldQueryGroup group, QueryConnection conn) {
    if (this.root == null) {
      this.root = group;
      return;
    }

    this.additionals.add(new Tuple<>(conn, group));
  }
}
