package me.blvckbytes.blvcksys.util.logging;

public interface ILogger {

  /**
   * Log a message on the debug level
   * @param format Format of the message
   * @param args Arguments to apply the format
   */
  void logDebug(String format, Object... args);

  /**
   * Log a message on the information level
   * @param format Format of the message
   * @param args Arguments to apply the format
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
   * @param args Arguments to apply the format
   */
  void logError(String format, Object... args);
}
