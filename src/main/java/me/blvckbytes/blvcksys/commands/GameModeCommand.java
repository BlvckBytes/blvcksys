package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 05/03/2022

  Change your own or another player's gamemode.
 */
@AutoConstruct
public class GameModeCommand extends APlayerCommand {

  public GameModeCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "gamemode,gm",
      "Change the gamemode of a player",
      PlayerPermission.COMMAND_GAMEMODE.toString(),
      new CommandArgument("<mode>", "GameMode to change to"),
      new CommandArgument("[player]", "Who to change the mode for", PlayerPermission.COMMAND_GAMEMODE_OTHERS.toString())
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      // Allow for numeric gamemode values
      return suggestEnum(args, currArg, GameMode.class, (acc, curr) -> {
        acc.add(curr.name());
        acc.add(String.valueOf(curr.getValue()));
      });

    if (currArg == 1)
      return suggestOnlinePlayers(p, args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    // Allow for numeric gamemode values
    GameMode mode = parseEnum(GameMode.class, args, 0, null, (repr, con) -> {
      if (con.name().equalsIgnoreCase(repr))
        return true;

      try {
        return con.getValue() == Integer.parseInt(repr);
      } catch (IllegalArgumentException ignored) {
        return false;
      }
    });

    Player target = onlinePlayer(args, 1, p);
    boolean isSelf = target.equals(p);
    GameMode prevMode = target.getGameMode();

    // Already in this gamemode
    if (mode.equals(prevMode)) {
      p.sendMessage(
        cfg.get(isSelf ? ConfigKey.GAMEMODE_SELF_HAS : ConfigKey.GAMEMODE_OTHERS_SENDER_HAS)
          .withPrefix()
          .withVariable("mode", mode.name())
          .withVariable("target", target.getName())
          .asScalar()
      );
      return;
    }

    // Set the gamemode of the target
    target.setGameMode(mode);

    // Inform the sender
    p.sendMessage(
      cfg.get(isSelf ? ConfigKey.GAMEMODE_SELF_SET : ConfigKey.GAMEMODE_OTHERS_SENDER_SET)
        .withPrefix()
        .withVariable("prev_mode", prevMode.name())
        .withVariable("curr_mode", mode.name())
        .withVariable("target", target.getName())
        .asScalar()
    );

    // Inform the target
    if (!isSelf)
      target.sendMessage(
        cfg.get(ConfigKey.GAMEMODE_OTHERS_RECEIVER)
          .withPrefix()
          .withVariable("prev_mode", prevMode.name())
          .withVariable("curr_mode", mode.name())
          .withVariable("issuer", p.getName())
          .asScalar()
      );
  }
}
