package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.handlers.IIgnoreHandler;
import me.blvckbytes.blvcksys.handlers.IPreferencesHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Send a private message to another player and keep track of the
  last player that the executor had a conversation with (message-partner).
  Remove this partnership if one of the two players leaves the server.
*/
@AutoConstruct
public class MsgCommand extends APlayerCommand implements IMsgCommand, Listener, IAutoConstructed {

  // Mapping sender to recipient and recipient to sender
  // B executes: /msg A results in: B->A, A->B
  private final Map<Player, Player> partners;

  private final IPreferencesHandler prefs;
  private final IIgnoreHandler ignores;
  private final IMsgSpyCommand spy;

  public MsgCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPreferencesHandler prefs,
    @AutoInject IIgnoreHandler ignores,
    @AutoInject IMsgSpyCommand spy
  ) {
    super(
      plugin, logger, cfg, refl,
      "msg",
      "Send a message to someone",
      null,
      new CommandArgument("<recipient>", "Message receiver"),
      new CommandArgument("<message>", "Message to send")
    );

    this.partners = new HashMap<>();

    this.prefs = prefs;
    this.ignores = ignores;
    this.spy = spy;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // First argument - provide all online players but the sender
    if (currArg == 0)
      return suggestOnlinePlayers(p, args, currArg, false, p);

    // Provide remaining args as placeholders (message is variadic)
    if (currArg >= 1)
      return Stream.of(getArgumentPlaceholder(currArg));

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  public void invoke(Player p, String label, String[] args) throws CommandException {
    // Get the recipient from the first argument
    Player recipient = onlinePlayer(args, 0);

    // Cannot send yourself messages
    if (recipient == p) {
      p.sendMessage(
        cfg.get(ConfigKey.MSG_SELF)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Sender has msg disabled
    if (prefs.isMsgDisabled(p)) {
      p.sendMessage(
        cfg.get(ConfigKey.MSG_DISABLED_SELF)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Receiver has msg disabled
    if (prefs.isMsgDisabled(recipient)) {
      p.sendMessage(
        cfg.get(ConfigKey.MSG_DISABLED_OTHERS)
          .withPrefix()
          .withVariable("receiver", recipient.getName())
          .asScalar()
      );
      return;
    }

    // The sender ignores this recipient
    if (ignores.getMsgIgnore(p, recipient)) {
      p.sendMessage(
        cfg.get(ConfigKey.IGNORE_MSG_IGNORING)
          .withPrefix()
          .withVariable("target", recipient.getName())
          .asScalar()
      );
      return;
    }

    // The recipient ignores this sender
    if (ignores.getMsgIgnore(recipient, p)) {
      p.sendMessage(
        cfg.get(ConfigKey.IGNORE_MSG_IGNORED)
          .withPrefix()
          .withVariable("target", recipient.getName())
          .asScalar()
      );
      return;
    }

    // Get the message
    String message = argvar(args, 1);

    // Send out the messages
    sendMessage(p, recipient, message);

    // Append them as partners
    this.partners.put(p, recipient);
    this.partners.put(recipient, p);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void cleanup() {
    // Remove all previous partners
    this.partners.clear();
  }

  @Override
  public void initialize() {}

  @Override
  public Player getPartner(Player sender) {
    return this.partners.get(sender);
  }

  @Override
  public void sendMessage(Player sender, Player receiver, String message) {
    receiver.sendMessage(
      cfg.get(ConfigKey.MSG_RECEIVER)
        .withPrefix()
        .withVariable("sender", sender.getDisplayName())
        .withVariable("message", message)
        .asScalar()
    );

    sender.sendMessage(
      cfg.get(ConfigKey.MSG_SENDER)
        .withPrefix()
        .withVariable("receiver", receiver.getDisplayName())
        .withVariable("message", message)
        .asScalar()
    );

    // Notify the spies if either the sender or the receiver is being spied on
    Stream.concat(
      spy.getSpies(sender).stream(),
      spy.getSpies(receiver).stream()
    ).forEach(s -> {
      s.sendMessage(
        cfg.get(ConfigKey.MSGSPY_MESSAGE)
          .withPrefix()
          .withVariable("sender", sender.getDisplayName())
          .withVariable("receiver", receiver.getDisplayName())
          .withVariable("message", message)
          .asScalar()
      );
    });
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Remove sender and receiver if one of both leaves the server
    Player partner = this.partners.remove(e.getPlayer());
    if (partner != null)
      this.partners.remove(partner);
  }
}
