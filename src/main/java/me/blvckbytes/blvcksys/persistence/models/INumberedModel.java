package me.blvckbytes.blvcksys.persistence.models;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  Describes a model which wants to receive it's counting
  number within a result.
*/
public interface INumberedModel {

  /**
   * Sets the number of this model within the result, starting
   * to count at one, from the first result
   * @param number Number within the result
   */
  void setResultNumber(int number);

}
