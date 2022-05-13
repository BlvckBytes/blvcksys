package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/13/2022

  A duration parameter could not be parsed and is thus invalid.
*/
public class InvalidDurationException extends CommandException {

  public InvalidDurationException(IConfig cfg, String duration) {
    super(
      cfg.get(ConfigKey.ERR_DURATIONPARSE)
        .withPrefix()
        .withVariable("duration", duration)
        .asScalar()
    );
  }
}
