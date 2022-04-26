package me.blvckbytes.blvcksys.util.cmd.exception;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

public class InvalidOptionException extends CommandException {

  /**
   * An option parameter could not be parsed and is thus invalid
   * @param cfg Config reference to retrieve the message template from
   * @param option Option that could not be parsed
   */
  public InvalidOptionException(IConfig cfg, String option) {
    super(
      cfg.get(ConfigKey.ERR_OPTIONPARSE)
        .withPrefix()
        .withVariable("option", option)
        .asScalar()
    );
  }
}
