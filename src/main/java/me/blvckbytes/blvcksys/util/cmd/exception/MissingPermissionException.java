package me.blvckbytes.blvcksys.util.cmd.exception;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;

public class MissingPermissionException extends CommandException {

  /**
   * A player does not have sufficient permissions to execute a command
   * @param cfg Config reference to retrieve the message template from
   * @param perm The permission that's missing
   */
  public MissingPermissionException(IConfig cfg, PlayerPermission perm) {
    super(
      cfg.get(ConfigKey.ERR_PERMISSION)
        .withPrefix()
        .withVariable("permission", perm.getValue())
        .asScalar()
    );
  }
}
