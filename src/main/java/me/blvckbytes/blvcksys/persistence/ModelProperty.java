package me.blvckbytes.blvcksys.persistence;

import me.blvckbytes.blvcksys.persistence.models.APersistentModel;

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

  // Whether this property's value has to be unique accross
  // all models of the same type
  boolean isUnique() default false;

  // Whether this field can be set to a NULL value
  boolean isNullable() default false;

  // Whether this field will be inherited
  boolean isInherited() default true;

  // Whether this field is inlined when using a transformer
  boolean isInlineable() default true;

  // What value to use when migrating this column to an existing data-structure
  MigrationDefault migrationDefault() default MigrationDefault.UNSPECIFIED;

  // Foreign key constraint target model, APersistentModel.class means none
  Class<? extends APersistentModel> foreignKey() default APersistentModel.class;
}
