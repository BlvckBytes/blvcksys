package me.blvckbytes.blvcksys.persistence;

import me.blvckbytes.blvcksys.persistence.models.APersistentModel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  Marks a field of a model as a row number receiver which itself is
  figured out at runtime and not stored in the table of the model.
*/
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RowNumber {

  /**
   * Specifies by what field of the model this row number
   * counter will be partitioned by, i.e. set to 1 again
   */
  String partitionedBy() default "id";
}
