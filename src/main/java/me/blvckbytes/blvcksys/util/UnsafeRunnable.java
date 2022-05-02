package me.blvckbytes.blvcksys.util;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Represents an unsafe runnable.
*/
@FunctionalInterface
public interface UnsafeRunnable {
  void run() throws Exception;
}
