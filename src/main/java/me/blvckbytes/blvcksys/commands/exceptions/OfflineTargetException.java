package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  This command was invoked towards a target that was offline
  but needed to be online for this operation to result in a success
*/
public class OfflineTargetException extends CommandException {

  public OfflineTargetException(IConfig cfg, String player) {
    super(
      cfg.get(ConfigKey.ERR_NOT_ONLINE)
        .withPrefix()
        .withVariable("player", player)
        .asScalar()
    );
  }
}
