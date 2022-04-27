package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

public class OfflineTargetException extends CommandException {

  /**
   * This command was invoked towards a target that was offline
   * but needed to be online for this operation to result in a success
   * @param cfg Config reference to retrieve the message template from
   * @param player Player that was not online
   */
  public OfflineTargetException(IConfig cfg, String player) {
    super(
      cfg.get(ConfigKey.ERR_NOT_ONLINE)
        .withPrefix()
        .withVariable("player", player)
        .asScalar()
    );
  }
}
