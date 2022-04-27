package me.blvckbytes.blvcksys.util.logging;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Public interfaces to handle logging of events on different levels.
*/
public interface ILogger {

  /**
   * Log a message on the debug level
   * @param format Format of the message
   * @param args Arguments to apply to the format
   */
  void logDebug(String format, Object... args);

  /**
   * Log a stringified object on the debug level
   * @param o Object to stringify
   * @param depth Depth of recursion
   */
  void logDebug(Object o, int depth);

  /**
   * Log a message on the information level
   * @param format Format of the message
   * @param args Arguments to apply to the format
   */
  void logInfo(String format, Object... args);

  /**
   * Log an exception on the error level
   * @param e Exception to log
   */
  void logError(Exception e);

  /**
   * Log a message on the error level
   * @param format Format of the message
   * @param args Arguments to apply to the format
   */
  void logError(String format, Object... args);
}
