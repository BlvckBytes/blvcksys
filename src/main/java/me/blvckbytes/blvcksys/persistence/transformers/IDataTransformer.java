package me.blvckbytes.blvcksys.persistence.transformers;

import me.blvckbytes.blvcksys.persistence.models.APersistentModel;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Represents the functionality of a data transformer that's used
  when handling foreign data for R/W.
*/
public interface IDataTransformer<Known extends APersistentModel, Foreign> {

  /**
   * Revive a known object back into it's foreign form it was in before persisting
   * @param data Known data loaded from persistence
   * @return Foreign data after the transformation
   */
  Foreign revive(Known data);

  /**
   * Replace a foreign object into it's known persistable representation before writing
   * @param data Foreign data to be stored
   * @return Known data to be saved
   */
  Known replace(Foreign data);

  /**
   * Get the known class (internal model)
   */
  Class<Known> getKnownClass();

  /**
   * Get the foreign class (external model)
   */
  Class<Foreign> getForeignClass();
}
