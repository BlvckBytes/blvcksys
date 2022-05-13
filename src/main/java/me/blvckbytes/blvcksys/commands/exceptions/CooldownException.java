package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigValue;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  A cooldown was still active while the executor tried to use this command.
*/
public class CooldownException extends CommandException {

  /**
   * Create a new cooldown exception which automatically formats
   * the provided duration in seconds to a more human readable time string
   * @param cval Config value which supports the message to print to the user
   * @param durationStr Duration formatted in a human readable way
   */
  public CooldownException(ConfigValue cval, String durationStr) {
    super(
      cval
        .withVariable("duration", durationStr)
        .asScalar()
    );
  }
}
