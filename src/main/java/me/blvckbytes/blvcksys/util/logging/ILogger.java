package me.blvckbytes.blvcksys.util.logging;

public interface ILogger {

  void logDebug(String format, Object... args);
  void logInfo(String format, Object... args);
  void logError(Exception e);
  void logError(String format, Object... args);

}
