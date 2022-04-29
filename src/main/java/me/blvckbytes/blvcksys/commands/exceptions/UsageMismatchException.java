package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import net.md_5.bungee.api.chat.BaseComponent;

import java.util.ArrayList;
import java.util.Arrays;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  This command was not invoked in a way that was conform to it's usage
*/
public class UsageMismatchException extends CommandException {

  public UsageMismatchException(IConfig cfg, BaseComponent[] advancedUsage) {
    super(
      // Prepend the advanced usage by the usage prefix
      prependComponent(
        cfg.get(ConfigKey.ERR_USAGE_PREFIX)
          .withPrefix()
          .asComponent(),
        advancedUsage
      )
    );
  }

  /**
   * Prepends the component a by component b
   * @param a Component to prepend by
   * @param arr Array to prepend
   * @return New head component
   */
  private static BaseComponent[] prependComponent(BaseComponent a, BaseComponent[] arr) {
    BaseComponent[] res = new BaseComponent[arr.length + 1];

    res[0] = a;
    System.arraycopy(arr, 0, res, 1, arr.length);

    return res;
  }
}
