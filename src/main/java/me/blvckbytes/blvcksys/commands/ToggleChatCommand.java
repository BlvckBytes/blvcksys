package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IObjectiveHandler;
import me.blvckbytes.blvcksys.handlers.IPreferencesHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Enable or disable receiving chat messages from other players.
*/
@AutoConstruct
public class ToggleChatCommand extends APlayerCommand {

  private final IPreferencesHandler prefs;

  public ToggleChatCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IObjectiveHandler obj,
    @AutoInject IPreferencesHandler prefs
  ) {
    super(
      plugin, logger, cfg, refl,
      "togglechat",
      "Enable or disable receiving player chat messages",
      null
    );

    this.prefs = prefs;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    boolean before = prefs.isChatHidden(p);
    prefs.setChatHidden(p, !before);

    p.sendMessage(
      cfg.get(!before ? ConfigKey.TOGGLECHAT_DISABLED : ConfigKey.TOGGLECHAT_ENABLED)
        .withPrefix()
        .asScalar()
    );
  }
}
