package me.blvckbytes.blvcksys.util.di;

import java.lang.annotation.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Marks a class to be intended for use with AutoConstructer.
*/
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoConstruct {}
