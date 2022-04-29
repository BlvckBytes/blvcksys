package me.blvckbytes.blvcksys.util.logging;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.ObjectStringifier;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.AutoInjectLate;
import org.bukkit.Bukkit;

import java.io.PrintWriter;
import java.io.StringWriter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Logs all events to the server's console and automatically prepends
  the level's configured prefix
*/
@AutoConstruct
public class ConsoleSenderLogger implements ILogger {

  private final IConfig cfg;

  @AutoInjectLate
  private ObjectStringifier ostr;

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
  public void logDebug(Object o, int depth) {
    if (ostr == null) {
      logError("The object stringifier is not yet available! (%s, %d)", o.getClass().getName(), depth);
      return;
    }

    log(cfg.get(ConfigKey.LOGGING_PREFIX_DEBUG) + ostr.stringifyObject(o, depth));
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
