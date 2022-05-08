package me.blvckbytes.blvcksys.persistence;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Specifies what happens when the foreign key referenced by the column get's deleted
*/
public enum ForeignKeyAction {
  DELETE_CASCADE,
  SET_NULL,
  RESTRICT
}
