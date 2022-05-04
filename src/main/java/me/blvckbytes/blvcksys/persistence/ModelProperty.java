package me.blvckbytes.blvcksys.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Marks a field of a model as one of it's properties to persist.
*/
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelProperty {

  // Representitive name of the field, defaults
  // to the field's name if not provided
  String name() default "";

  // Whether this property's value has to be unique accross
  // all models of the same type
  boolean isUnique() default false;

  // Whether this property ignores casing
  // Only applicable to Strings
  boolean ignoreCasing() default true;
}
