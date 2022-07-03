package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/05/2022

  Teleport yourself or another player to specific coordinates instantly.
*/
@AutoConstruct
public class TpcCommand extends APlayerCommand {

  public TpcCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "tpc",
      "Teleport to specific coordinates instantly",
      PlayerPermission.COMMAND_TPC.toString(),
      new CommandArgument("<x>", "X coordinate"),
      new CommandArgument("<y>", "Y coordinate"),
      new CommandArgument("<z>", "Z coordinate"),
      new CommandArgument("[player]", "Player to teleport, defaults to yourself", PlayerPermission.COMMAND_TPC_OTHERS.toString())
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg < 3)
      return Stream.of(getArgumentPlaceholder(currArg));
    if (currArg == 3)
      return suggestOnlinePlayers(p, args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    float x = parseFloat(args, 0), y = parseFloat(args, 1), z = parseFloat(args, 2);
    Player player = onlinePlayer(args, 3, p);
    boolean isSelf = player.equals(p);

    Location l = new Location(p.getLocation().getWorld(), x, y, z);

    // Teleport to the target
    player.teleport(l);

    // Inform the command sender
    p.sendMessage(
      cfg.get(isSelf ? ConfigKey.TPC_SELF : ConfigKey.TPC_OTHER_SENDER)
        .withPrefix()
        .withVariable("location", "(" + l.getX() + " | " + l.getY() + " | " + l.getZ() + ")")
        .withVariable("player", player.getName())
        .asScalar()
    );

    // Inform the teleported player
    if (!isSelf)
      player.sendMessage(
        cfg.get(ConfigKey.TPC_OTHER_RECEIVER)
          .withPrefix()
          .withVariable("location", "(" + l.getX() + " | " + l.getY() + " | " + l.getZ() + ")")
          .withVariable("issuer", p.getName())
          .asScalar()
      );
  }
}
