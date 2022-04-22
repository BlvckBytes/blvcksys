package me.blvckbytes.blvcksys.util.di;

import java.lang.annotation.*;

/**
 * Marks a class to be intended for use with AutoConstructer
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoConstruct {}
