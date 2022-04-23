package me.blvckbytes.blvcksys.util.di;

/**
 * Represents an @AutoConstruct'ed resource
 */
public interface IAutoConstructed {

  /**
   * Called to clean up before the instance is about to be destroyed
   */
  void cleanup();

  /**
   * Called to initialize after all resources have been constructed
   */
  void initialize();
}
