package me.blvckbytes.blvcksys.util.logging;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Public interfaces to handle logging of events on different levels.
*/
public interface ILogger {

  /**
   * Log a message on the debug level
   * @param message Message to log
   */
  void logDebug(String message);

  /**
   * Log a stringified object on the debug level
   * @param o Object to stringify
   * @param depth Depth of recursion
   */
  void logDebug(Object o, int depth);

  /**
   * Log a message on the information level
   * @param message Message to log
   */
  void logInfo(String message);

  /**
   * Log an exception on the error level
   * @param e Exception to log
   */
  void logError(Exception e);

  /**
   * Log a message on the error level
   * @param message Message to log
   */
  void logError(String message);
}
