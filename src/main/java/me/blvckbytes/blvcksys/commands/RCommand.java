package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.cmd.CommandResult;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

@AutoConstruct
public class RCommand extends APlayerCommand {

  private final IMsgCommand msgC;

  public RCommand(
    @AutoInject JavaPlugin main,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IMsgCommand msgC
  ) {
    super(
      main, logger, cfg, refl,
      "r",
      "Respond to the last message",
      new String[][] {
        { "<message>", "Message to send" }
      },
      "reply"
    );

    this.msgC = msgC;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  public CommandResult onInvocation(Player p, String label, String[] args) {
    if (args.length == 0)
      return usageMismatch();

    // Ensure there's an active partner on the other end
    Player partner = this.msgC.getPartner(p);
    if (partner == null)
      return customError(cfg.getP(ConfigKey.MSG_NO_PARTNER));

    // Send out the messages
    String message = argvar(args, 0);
    this.msgC.sendMessage(p, partner, message);

    return success();
  }

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Provide placeholder (message is variadic)
    return Stream.of(getArgumentPlaceholder(currArg));
  }
}
