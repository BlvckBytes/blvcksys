package me.blvckbytes.blvcksys.di;

import java.util.Collection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Public interfaces which the auto-constructer provides to other consumers.
*/
public interface IAutoConstructer {

  /**
   * Get all instantiated {@link AutoConstruct} resources
   */
  Collection<Object> getAllInstances();

  /**
   * Register a callback to be executed as soon as all resources have
   * been instantiated and the constructer has reached a state of completion
   * @param r Runnable to call
   */
  void onCompletion(Runnable r);
}
