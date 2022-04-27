package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/27/2022

  This command was invoked towards an offline target that was supposed
  to be a known player, but actually never played on this server
*/
public class UnknownTargetException extends CommandException {

  public UnknownTargetException(IConfig cfg, String player) {
    super(
      cfg.get(ConfigKey.ERR_PLAYER_UNKNOWN)
        .withPrefix()
        .withVariable("player", player)
        .asScalar()
    );
  }
}
