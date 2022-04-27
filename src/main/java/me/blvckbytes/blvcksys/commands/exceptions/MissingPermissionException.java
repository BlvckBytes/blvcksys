package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  A player does not have sufficient permissions to execute a command
*/
public class MissingPermissionException extends CommandException {

  public MissingPermissionException(IConfig cfg, PlayerPermission perm) {
    super(
      cfg.get(ConfigKey.ERR_PERMISSION)
        .withPrefix()
        .withVariable("permission", perm.getValue())
        .asScalar()
    );
  }
}
