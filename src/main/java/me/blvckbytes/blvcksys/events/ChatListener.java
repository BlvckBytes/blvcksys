package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.handlers.IPreferencesHandler;
import me.blvckbytes.blvcksys.handlers.ITeamHandler;
import me.blvckbytes.blvcksys.packets.communicators.team.TeamGroup;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  Cancels vanilla chat packets and performs a custom broadcast
  to all recipients using a custom format specified in the config.
  Prefixes are loaded from the tab-list, colors are translated
  based on the messenger's permissions.
*/
@AutoConstruct
public class ChatListener implements Listener, IChatListener {

  private final IConfig cfg;
  private final ITeamHandler teams;
  private final IPreferencesHandler prefs;

  public ChatListener(
    @AutoInject IConfig cfg,
    @AutoInject ITeamHandler teams,
    @AutoInject IPreferencesHandler prefs
  ) {
    this.cfg = cfg;
    this.teams = teams;
    this.prefs = prefs;
  }

  //=========================================================================//
  //                                   API                                  //
  //=========================================================================//

  @Override
  public void sendChatMessage(Player sender, Collection<? extends Player> receivers, String message) {
    // Get the sender's group
    Optional<TeamGroup> tg = teams.getPlayerGroup(sender);
    String prefix = tg.map(TeamGroup::prefix).orElse("§r");

    // Broadcast to all receivers
    boolean senderBypassesToggleChat = PlayerPermission.TOGGLECHAT_BYPASS.has(sender);
    for(Player receiver : receivers) {

      // Don't send messages to players who have disabled their
      // chat, except for important messages from bypassing players
      if (
        prefs.isChatHidden(receiver) &&
        !senderBypassesToggleChat &&

        // Skip blocking self
        receiver != sender
      )
        continue;

      // Override the default message
      receiver.sendMessage(
        cfg.get(ConfigKey.CHAT_FORMAT)
          .withVariable("name", sender.getName())
          .withVariable("message", translateColors(sender, message, PlayerPermission.CHAT_COLOR_PREFIX))
          .withVariable("prefix", prefix)
          .asScalar()
      );
    }
  }

  @Override
  public void broadcastMessage(Collection<? extends Player> receivers, String message) {
    for (Player t : receivers)
      t.sendMessage(message);
  }

  @Override
  public void broadcastMessage(Collection<? extends Player> receivers, TextComponent message) {
    for (Player t : receivers)
      t.spigot().sendMessage(message);
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onChat(AsyncPlayerChatEvent e) {
    Player p = e.getPlayer();

    // Cancel the vanilla event
    e.setCancelled(true);

    // Send using custom formatting
    sendChatMessage(p, new ArrayList<>(e.getRecipients()), e.getMessage());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  @Override
  public String translateColors(Player p, String message, PlayerPermission prefix) {
    StringBuilder res = new StringBuilder();

    for (int i = 0; i < message.length(); i++) {
      char curr = message.charAt(i);
      res.append(curr);

      // Only look for color indicators
      if (curr != '&')
        continue;

      // End of message reached, cannot translate
      if (i == message.length() - 1)
        continue;

      // Get the desired color's char
      char colorChar = message.charAt(i + 1);
      ChatColor color = ChatColor.getByChar(colorChar);

      // Unknown color, skip
      if (color == null)
        continue;

      // Player cannot use this color, skip
      if (!prefix.has(p, color.name().toLowerCase()))
        continue;

      // Substitute color character to enable this color
      res.deleteCharAt(i);
      res.append("§");
    }

    return res.toString();
  }
}
