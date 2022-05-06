package me.blvckbytes.blvcksys.persistence.query;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  5reated On: 05/06/2022

  Conveniently wraps an equality query for a given field into a record.
*/
public record FieldQuery(
  String field,
  EqualityOperation op,
  Object value
) {}
