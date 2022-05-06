package me.blvckbytes.blvcksys.persistence.query;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Represents all existing equality operations that can be performed on various columns.
*/
public enum EqualityOperation {
  // Equals
  EQ,

  // Equals ignorecase
  EQ_IC,

  // Not equals
  NEQ,

  // Not equals ignorecase
  NEQ_IC,

  // Greater than
  GT,

  // Less than
  LT,

  // Greater than or equal
  GTE,

  // Less than or equal
  LTE
}
