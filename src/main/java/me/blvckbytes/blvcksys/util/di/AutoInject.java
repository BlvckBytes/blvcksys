package me.blvckbytes.blvcksys.util.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Marks a constructor parameter to be a @AutoConstruct dependency.
*/
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoInject {

  /**
   * Whether to init this dependency "late" (as soon as
   * possible), which is used to break circular dependencies that
   * do not require to be instantly resolved. This mode does require
   * a local member field that has the exact same type as the
   * dependency to be injected.
   *
   * WARNING: This also means that until this dependency becomes
   * available, null will be injected (account for that!)
   */
  boolean lateinit() default false;
}
