package me.blvckbytes.blvcksys.persistence;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.APersistentModel;

import java.util.List;
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
   void delete(APersistentModel model) throws PersistenceException;

  /**
   * Delete a previously created model by it's id
   * @param id ID of the model
   */
  <T extends APersistentModel>void delete(Class<T> type, UUID id) throws PersistenceException;
}
