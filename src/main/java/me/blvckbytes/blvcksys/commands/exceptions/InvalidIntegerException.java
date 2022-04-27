package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

public class InvalidIntegerException extends CommandException {

  /**
   * An integer parameter could not be parsed and is thus invalid
   * @param cfg Config reference to retrieve the message template from
   * @param number Number that could not be parsed
   */
  public InvalidIntegerException(IConfig cfg, String number) {
    super(
      cfg.get(ConfigKey.ERR_INTPARSE)
        .withPrefix()
        .withVariable("number", number)
        .asScalar()
    );
  }
}
