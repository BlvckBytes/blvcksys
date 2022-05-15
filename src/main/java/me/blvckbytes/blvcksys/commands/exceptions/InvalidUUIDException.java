package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/16/2022

  A UUID parameter could not be parsed and is thus invalid.
*/
public class InvalidUUIDException extends CommandException {

  public InvalidUUIDException(IConfig cfg, String uuid) {
    super(
      cfg.get(ConfigKey.ERR_UUIDPARSE)
        .withPrefix()
        .withVariable("uuid", uuid)
        .asScalar()
    );
  }
}
