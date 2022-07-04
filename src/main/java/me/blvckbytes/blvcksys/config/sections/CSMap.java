package me.blvckbytes.blvcksys.config.sections;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Marks a class' field to be parsed as a literal map and
  specifies the map's types.
*/
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CSMap {
  Class<?> k();
  Class<?> v();
}
