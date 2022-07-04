package me.blvckbytes.blvcksys.config.sections;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/01/2022

  Marks a class' field to always be present, no matter if the config
  had the required key. If it's missing, an empty default is still created.
*/
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CSAlways {}
