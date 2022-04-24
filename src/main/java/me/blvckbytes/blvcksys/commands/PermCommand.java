package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.cmd.CommandResult;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

@AutoConstruct
public class PermCommand extends APlayerCommand {

  public PermCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "perm",
      "Check if you have a permission",
      new String[][] {
        { "<permission>", "The permission to check for" }
      }
    );
  }

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return Stream.of(getArgumentPlaceholder(currArg));
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected CommandResult onInvocation(Player p, String label, String[] args) {
    if (args.length != 1)
      return usageMismatch();

    String perm = args[0];
    p.sendMessage(p.hasPermission(perm) ? "§aYou have " + perm : "§cYou don't have " + perm);

    return success();
  }
}
