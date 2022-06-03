package me.blvckbytes.blvcksys.persistence;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.APersistentModel;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Represents the functionality a persistence implementation has to offer.
*/
public interface IPersistence {

  /**
   * Store a model persistently
   * @param model Model to store
   */
  void store(APersistentModel model) throws PersistenceException;

  /**
   * List all available models of a certain type
   * @param type Type of model to list
   * @return List of all available records
   */
   <T extends APersistentModel> List<T> list(Class<T> type) throws PersistenceException;

  /**
   * Delete a previously created model
   * @param model Model to delete
   */
   boolean delete(APersistentModel model) throws PersistenceException;

  /**
   * Delete models by a query
   * @param query Query that specifies what to delete
   */
  <T extends APersistentModel> int delete(QueryBuilder<T> query) throws PersistenceException;

  /**
   * Delete a previously created model by it's id
   * @param id ID of the model
   */
  <T extends APersistentModel>boolean delete(Class<T> type, UUID id) throws PersistenceException;

  /**
   * Find all models that match the specified query
   * @param query Query to execute
   * @return List of models
   */
  <T extends APersistentModel> List<T> find(QueryBuilder<T> query) throws PersistenceException;

  /**
   * Count all models that match the specified query
   * @param query Query to execute
   */
  <T extends APersistentModel> int count(QueryBuilder<T> query) throws PersistenceException;

  /**
   * Count all models of a specific type
   * @param type Type of model to count
   */
  <T extends APersistentModel> int count(Class<T> type) throws PersistenceException;

  /**
   * Find the first model that matches the specified query
   * @param query Query to execute
   * @return First model, empty if there were no matches
   */
  <T extends APersistentModel> Optional<T> findFirst(QueryBuilder<T> query) throws PersistenceException;

  /**
   * Get a set of properties for all models that match
   * the specified query in their raw, unwrapped form
   * @param query Query to execute
   * @param properties Properties to receive within the map
   * @return List of properties
   */
  <T extends APersistentModel> List<Map<String, Object>> findRaw(QueryBuilder<T> query, String... properties);

  /**
   * Get a set of properties for all models that are available
   * @param type Type of model to list
   * @param properties Properties to receive within the map
   * @return List of properties for all available items
   */
  <T extends APersistentModel> List<Map<String, Object>> listRaw(Class<T> type, String... properties);
}
