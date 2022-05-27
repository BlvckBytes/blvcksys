package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Spy on the private messages of other players by receiving carbon copies.
*/
@AutoConstruct
public class MsgSpyCommand extends APlayerCommand implements IMsgSpyCommand, Listener {

  // Mapping a spy target to a list of spies
  private final Map<Player, List<Player>> spySessions;

  public MsgSpyCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "msgspy",
      "Get a copy of all private messages of a target",
      PlayerPermission.COMMAND_MSGSPY,
      new CommandArgument("<target>", "Target to spy on")
    );

    this.spySessions = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public List<Player> getSpies(Player target) {
    return spySessions.getOrDefault(target, new ArrayList<>());
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOnlinePlayers(p, args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0);

    if (target.equals(p)) {
      p.sendMessage(
        cfg.get(ConfigKey.MSGSPY_SELF)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    if (!spySessions.containsKey(target))
      spySessions.put(target, new ArrayList<>());

    List<Player> spies = spySessions.get(target);

    p.sendMessage(
      cfg.get(spies.contains(p) ? ConfigKey.MSGSPY_DISABLED : ConfigKey.MSGSPY_ENABLED)
        .withPrefix()
        .withVariable("target", target.getName())
        .asScalar()
    );

    if (spies.contains(p))
      spies.remove(p);
    else
      spies.add(p);
  }

  //=========================================================================//
  //                                 Listener                                //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    spySessions.remove(e.getPlayer());
  }
}
