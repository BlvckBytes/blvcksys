package me.blvckbytes.blvcksys.util.logging;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.ObjectStringifier;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
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

  private void log(String message) {
    Bukkit.getConsoleSender().sendMessage(
      cfg.get(ConfigKey.PREFIX) + message
    );
  }

  @Override
  public void logDebug(String message) {
    log(cfg.get(ConfigKey.LOGGING_PREFIX_DEBUG) + message);
  }

  @Override
  public void logDebug(Object o, int depth) {
    if (ostr == null) {
      logError("The object stringifier is not yet available! (" + o + ", " + depth + ")");
      return;
    }

    log(cfg.get(ConfigKey.LOGGING_PREFIX_DEBUG) + ostr.stringifyObject(o, depth));
  }

  @Override
  public void logInfo(String message) {
    log(cfg.get(ConfigKey.LOGGING_PREFIX_INFO) + message);
  }

  @Override
  public void logError(Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    logError(sw.toString());
  }

  @Override
  public void logError(String message) {
    log(cfg.get(ConfigKey.LOGGING_PREFIX_ERROR) + message);
  }
}
