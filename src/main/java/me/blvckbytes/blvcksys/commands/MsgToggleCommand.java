package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPreferencesHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  Toggles whether you want to receive private messages.
*/
@AutoConstruct
public class MsgToggleCommand extends APlayerCommand {

  private final IPreferencesHandler prefs;

  public MsgToggleCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPreferencesHandler prefs
  ) {
    super(
      plugin, logger, cfg, refl,
      "msgtoggle",
      "Toggle whether you'll receive private messages",
      null
    );

    this.prefs = prefs;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    boolean state = !prefs.isMsgDisabled(p);
    prefs.setMsgDisabled(p, state);

    p.sendMessage(
      cfg.get(state ? ConfigKey.MSGTOGGLE_DISABLED : ConfigKey.MSGTOGGLE_ENABLED)
        .withPrefix()
        .asScalar()
    );
  }
}
