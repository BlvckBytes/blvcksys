package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  An option parameter could not be parsed and is thus invalid
*/
public class InvalidOptionException extends CommandException {

  public InvalidOptionException(IConfig cfg, String option) {
    super(
      cfg.get(ConfigKey.ERR_OPTIONPARSE)
        .withPrefix()
        .withVariable("option", option)
        .asScalar()
    );
  }
}
