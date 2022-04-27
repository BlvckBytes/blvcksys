package me.blvckbytes.blvcksys.util.cmd.exception;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

public class UnknownTargetException extends CommandException {

  /**
   * This command was invoked towards an offline target that was supposed
   * to be a known player, but actually never played on this server
   * @param cfg Config reference to retrieve the message template from
   * @param player Player that was not known
   */
  public UnknownTargetException(IConfig cfg, String player) {
    super(
      cfg.get(ConfigKey.ERR_PLAYER_UNKNOWN)
        .withPrefix()
        .withVariable("player", player)
        .asScalar()
    );
  }
}
