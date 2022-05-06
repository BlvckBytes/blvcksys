package me.blvckbytes.blvcksys.persistence.query;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Represents all existing logical query connections.
*/
public enum QueryConnection {
  // The previous AND the following queries have to match
  AND,

  // The previous OR the following query, or both, have to match
  OR
}
