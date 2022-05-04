package me.blvckbytes.blvcksys.persistence.exceptions;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  Represents an exception that occurred while operating on the persistence layer.
*/
public class PersistenceException extends RuntimeException {

  public PersistenceException(String message) {
    super(message);
  }
}
