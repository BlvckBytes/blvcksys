package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Send a private message to the executor's current message-partner,
  specified by either the last sent or the last received message.
*/
@AutoConstruct
public class RCommand extends APlayerCommand {

  private final IMsgCommand msgC;

  public RCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IMsgCommand msgC
  ) {
    super(
      plugin, logger, cfg, refl,
      "r",
      "Respond to the last message",
      null,
      new CommandArgument("<message>", "Message to send")
    );

    this.msgC = msgC;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  public void invoke(Player p, String label, String[] args) throws CommandException {
    // Ensure there's an active partner on the other end
    Player partner = this.msgC.getPartner(p);
    if (partner == null)
      customError(
        cfg.get(ConfigKey.MSG_NO_PARTNER)
          .withPrefix()
          .asScalar()
      );

    // Send out the messages
    String message = argvar(args, 0);
    this.msgC.sendMessage(p, partner, message);
  }

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Provide placeholder (message is variadic)
    return Stream.of(getArgumentPlaceholder(currArg));
  }
}
