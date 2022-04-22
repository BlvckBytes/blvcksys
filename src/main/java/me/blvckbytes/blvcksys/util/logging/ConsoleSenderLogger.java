package me.blvckbytes.blvcksys.util.logging;

import org.bukkit.Bukkit;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ConsoleSenderLogger implements ILogger {

  private final String prefix;
  private final boolean debug;

  public ConsoleSenderLogger(String prefix, boolean debug) {
    this.prefix = prefix;
    this.debug = debug;
  }

  private void log(String format, Object... args) {
    Bukkit.getConsoleSender().sendMessage(
      this.prefix + format.formatted(args)
    );
  }

  @Override
  public void logDebug(String format, Object... args) {
    if (!debug)
      return;

    log("§6%s".formatted(format.formatted(args)));
  }

  @Override
  public void logInfo(String format, Object... args) {
    log("§a%s".formatted(format.formatted(args)));
  }

  @Override
  public void logError(Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    log("§c%s".formatted(sw.toString()));
  }

  @Override
  public void logError(String format, Object... args) {
    log("§c%s".formatted(format.formatted(args)));
  }
}
