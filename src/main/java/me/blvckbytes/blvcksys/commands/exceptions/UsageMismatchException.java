package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import net.md_5.bungee.api.chat.BaseComponent;

public class UsageMismatchException extends CommandException {

  /**
   * This command was not invoked in a way that was conform to it's usage
   */
  public UsageMismatchException(IConfig cfg, BaseComponent advancedUsage) {
    super(
      // Prepend the advanced usage by the usage prefix
      prependComponent(
        advancedUsage,
        cfg.get(ConfigKey.ERR_USAGE_PREFIX)
          .withPrefix()
          .asComponent()
      )
    );
  }

  /**
   * Prepends the component a by component b
   * @param a Component to be prepended
   * @param b Component to prepend by
   * @return New head component
   */
  private static BaseComponent prependComponent(BaseComponent a, BaseComponent b) {
    b.addExtra(a);
    return b;
  }
}
