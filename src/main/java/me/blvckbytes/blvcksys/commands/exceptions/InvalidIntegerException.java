package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  An integer parameter could not be parsed and is thus invalid.
*/
public class InvalidIntegerException extends CommandException {

  public InvalidIntegerException(IConfig cfg, String number) {
    super(
      cfg.get(ConfigKey.ERR_INTPARSE)
        .withPrefix()
        .withVariable("number", number)
        .asScalar()
    );
  }
}
