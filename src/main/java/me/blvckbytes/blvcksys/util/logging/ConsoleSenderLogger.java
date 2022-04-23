package me.blvckbytes.blvcksys.util.logging;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import org.bukkit.Bukkit;

import java.io.PrintWriter;
import java.io.StringWriter;

@AutoConstruct
public class ConsoleSenderLogger implements ILogger {

  private final IConfig cfg;

  public ConsoleSenderLogger(
    @AutoInject IConfig cfg
  ) {
    this.cfg = cfg;
  }

  private void log(String format, Object... args) {
    Bukkit.getConsoleSender().sendMessage(
      cfg.get(ConfigKey.PREFIX) + format.formatted(args)
    );
  }

  @Override
  public void logDebug(String format, Object... args) {
    log(cfg.get(ConfigKey.LOGGING_PREFIX_DEBUG) + format.formatted(args));
  }

  @Override
  public void logInfo(String format, Object... args) {
    log(cfg.get(ConfigKey.LOGGING_PREFIX_INFO) + format.formatted(args));
  }

  @Override
  public void logError(Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    logError(sw.toString());
  }

  @Override
  public void logError(String format, Object... args) {
    log(cfg.get(ConfigKey.LOGGING_PREFIX_ERROR) + format.formatted(args));
  }
}
