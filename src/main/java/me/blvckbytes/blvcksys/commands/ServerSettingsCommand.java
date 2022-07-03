package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IServerSettingsHandler;
import me.blvckbytes.blvcksys.handlers.ServerSettingsHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Exposes the server settings as a command interface to get and set values.
*/
@AutoConstruct
public class ServerSettingsCommand extends APlayerCommand {

  private enum SettingAction {
    GET, SET
  }

  private enum ServerSetting {
    ATTACK_SPEED
  }

  private final IServerSettingsHandler settings;

  public ServerSettingsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ServerSettingsHandler settings
  ) {
    super(
      plugin, logger, cfg, refl,
      "serversettings",
      "Get or change server settings",
      PlayerPermission.COMMAND_SERVERSETTINGS.toString(),
      new CommandArgument("<action>", "Action to perform"),
      new CommandArgument("<setting>", "Name of the setting"),
      new CommandArgument("[value]", "Value to write")
    );

    this.settings = settings;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestEnum(args, currArg, SettingAction.class);

    if (currArg == 1)
      return suggestEnum(args, currArg, ServerSetting.class);

    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    SettingAction action = parseEnum(SettingAction.class, args, 0, null);
    ServerSetting setting = parseEnum(ServerSetting.class, args, 1, null);

    if (action == SettingAction.GET) {
      p.sendMessage(
        cfg.get(ConfigKey.SERVER_SETTINGS_GET)
          .withPrefix()
          .withVariable("setting", setting.name())
          .withVariable(
            "value",
            switch (setting) {
              case ATTACK_SPEED -> settings.getAttackSpeed();
            }
          )
          .asScalar()
      );
      return;
    }

    p.sendMessage(
      cfg.get(ConfigKey.SERVER_SETTINGS_GET)
        .withPrefix()
        .withVariable("setting", setting.name())
        .withVariable(
          "value",
          switch (setting) {
            case ATTACK_SPEED -> {
              int value = parseInt(args, 2);
              settings.setAttackSpeed(value);
              yield value;
            }
          }
        )
        .asScalar()
    );
  }
}
