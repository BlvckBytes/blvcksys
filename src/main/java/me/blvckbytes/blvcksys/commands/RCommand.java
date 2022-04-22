package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.config.Config;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.cmd.CommandResult;
import org.bukkit.entity.Player;

@AutoConstruct
public class RCommand extends APlayerCommand {

  private final IMsgCommand msgC;

  public RCommand(
    @AutoInject IMsgCommand msgC
  ) {
    super(
      "r",
      "Respond to the last message",
      "/r <Message>",
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
      return customError(Config.getP(ConfigKey.MSG_NO_PARTNER));

    // Send out the messages
    String message = argvar(args, 0);
    this.msgC.sendMessage(p, partner, message);

    return success();
  }
}
