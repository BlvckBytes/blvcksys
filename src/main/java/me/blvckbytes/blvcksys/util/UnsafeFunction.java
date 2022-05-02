package me.blvckbytes.blvcksys.util;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Represents an unsafe lambda function.
*/
@FunctionalInterface
public interface UnsafeFunction<I, O> {
  O apply(I val) throws Exception;
}
