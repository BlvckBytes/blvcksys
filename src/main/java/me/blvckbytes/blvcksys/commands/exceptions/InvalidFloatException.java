package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/07/2022

  A floating point parameter could not be parsed and is thus invalid.
*/
public class InvalidFloatException extends CommandException {

  public InvalidFloatException(IConfig cfg, String number) {
    super(
      cfg.get(ConfigKey.ERR_FLOATPARSE)
        .withPrefix()
        .withVariable("number", number)
        .asScalar()
    );
  }
}
