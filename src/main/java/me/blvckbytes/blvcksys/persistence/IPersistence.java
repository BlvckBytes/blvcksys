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
   * Store a model persistently
   * @param id Target identifier
   * @return Model corresponding to the requested ID
   */
  APersistentModel findById(UUID id) throws PersistenceException;

  /**
   * Store a model persistently
   * @param id Target identifier
   * @return Model corresponding to the requested ID
   */
  APersistentModel findById(String id) throws PersistenceException;

  /**
   * List all available models of a certain type
   * @param type Type of model to list
   * @return List of all available records
   */
   <T extends APersistentModel> List<T> list(Class<T> type);
}
