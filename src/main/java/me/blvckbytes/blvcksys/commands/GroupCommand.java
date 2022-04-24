package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.managers.Group;
import me.blvckbytes.blvcksys.managers.IGroupManager;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.cmd.CommandResult;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.stream.Stream;

@AutoConstruct
public class GroupCommand extends APlayerCommand {

  private enum Action {
    ADD,
    REMOVE,
    LIST
  }

  private final IGroupManager gm;

  public GroupCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IGroupManager gm
  ) {
    super(
      plugin, logger, cfg, refl,
      "group",
      "Manage groups on the server",
      new String[][]{
        { "<add/remove/list>", "Action to perform" },
        { "[group]", "Name of the group" },
        { "[player]", "Target player" }
      }
    );

    this.gm = gm;
  }

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // First argument - provide all action enum values
    if (currArg == 0)
      return Arrays.stream(Action.values())
        .map(Enum::toString)
        .filter(m -> m.toLowerCase().contains(args[currArg].toLowerCase()));

    // Second argument - provide all groups
    else if (currArg == 1)
      return gm.getGroups()
        .stream()
        .map(Group::name)
        .filter(m -> m.toLowerCase().contains(args[currArg].toLowerCase()));

    // Third argument - provide all online players
    else if (currArg == 2)
      return Bukkit.getOnlinePlayers()
        .stream()
        .map(Player::getDisplayName);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected CommandResult onInvocation(Player p, String label, String[] args) {
    if (args.length == 0)
      return usageMismatch();

    Action action;

    // Try to parse the action from the local enum
    try {
      action = Action.valueOf(args[0].toUpperCase());
    } catch (Exception e) {
      return usageMismatch();
    }

    switch (action) {
      case LIST -> {
        p.sendMessage(String.join(", ", gm.getGroups().stream().map(Group::name).toList()));
      }

      case ADD, REMOVE -> {
        if (args.length != 3)
          return usageMismatch();

        // Group not existing
        Group group = gm.getGroup(args[1]);
        if (group == null)
          return customError("Group not existing: %s".formatted(args[1]));

        // Target not online
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null)
          return playerOffline(args[2]);

        if (action == Action.ADD) {
          boolean success = gm.addToGroup(target, group);

          if (success)
            p.sendMessage("%s added to %s".formatted(target.getName(), group.name()));
          else
            p.sendMessage("%s already in %s".formatted(target.getName(), group.name()));
        }

        else {
          boolean success = gm.removeFromGroup(target, group);

          if (success)
            p.sendMessage("%s removed from %s".formatted(target.getName(), group.name()));
          else
            p.sendMessage("%s not in %s".formatted(target.getName(), group.name()));
        }
      }
    }

    return success();
  }
}
