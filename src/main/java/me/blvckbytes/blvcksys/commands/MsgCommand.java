package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.cmd.CommandResult;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@AutoConstruct
public class MsgCommand extends APlayerCommand implements IMsgCommand, Listener {

  // Mapping sender to recipient and recipient to sender
  // B executes: /msg A results in: B->A, A->B
  private final Map<Player, Player> partners;

  public MsgCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "msg",
      "Send a message to someone",
      new String[][] {
        { "<recipient>", "Message receiver" },
        { "<message>", "Message to send" }
      }
    );

    this.partners = new HashMap<>();
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // First argument - provide all online players but the sender
    if (currArg == 0)
      return Bukkit.getOnlinePlayers()
        .stream()
        .filter(pl -> pl != p)
        .map(Player::getDisplayName)
        .filter(n -> n.toLowerCase().contains(args[currArg].toLowerCase()));

    // Provide remaining args as placeholders (message is variadic)
    if (currArg >= 1)
      return Stream.of(getArgumentPlaceholder(currArg));

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  public CommandResult onInvocation(Player p, String label, String[] args) {
    if (args.length < 2)
      return usageMismatch();

    // Get the recipient from the first argument
    Player recipient = Bukkit.getPlayer(args[0]);
    if (recipient == null)
      return playerOffline(args[0]);

    // Cannot send yourself messages
    if (recipient == p)
      return customError(cfg.getP(ConfigKey.MSG_SELF));

    // Get the message
    String message = argvar(args, 1);

    // Send out the messages
    sendMessage(p, recipient, message);

    // Append them as partners
    this.partners.put(p, recipient);
    this.partners.put(recipient, p);

    return success();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public Player getPartner(Player sender) {
    return this.partners.get(sender);
  }

  @Override
  public void sendMessage(Player sender, Player receiver, String message) {
    receiver.sendMessage(cfg.get(ConfigKey.MSG_RECEIVER, sender.getDisplayName(), message));
    sender.sendMessage(cfg.get(ConfigKey.MSG_SENDER, receiver.getDisplayName(), message));
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
