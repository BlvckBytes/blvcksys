package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IIgnoreHandler;
import me.blvckbytes.blvcksys.handlers.IMuteHandler;
import me.blvckbytes.blvcksys.handlers.IPreferencesHandler;
import me.blvckbytes.blvcksys.handlers.ITeamHandler;
import me.blvckbytes.blvcksys.packets.communicators.team.TeamGroup;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.models.MuteModel;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
  private final IIgnoreHandler ignores;

  @AutoInjectLate
  private IMuteHandler mutes;

  public ChatListener(
    @AutoInject IConfig cfg,
    @AutoInject ITeamHandler teams,
    @AutoInject IPreferencesHandler prefs,
    @AutoInject IIgnoreHandler ignores
  ) {
    this.cfg = cfg;
    this.teams = teams;
    this.prefs = prefs;
    this.ignores = ignores;
  }

  //=========================================================================//
  //                                   API                                  //
  //=========================================================================//

  @Override
  public void sendChatMessage(Player sender, Collection<? extends Player> receivers, String message) {
    // Get the sender's group
    Optional<TeamGroup> tg = teams.getPlayerGroup(sender);
    String prefix = tg.map(TeamGroup::prefix).orElse("§r");

    // Translate chat colors based on permissions
    message = translateColors(sender, message, PlayerPermission.CHAT_COLOR_PREFIX.toString());

    // Set a default message color
    message = cfg.get(ConfigKey.CHAT_MESSAGE_DEF_COLOR).asScalar() + message;

    // Tag online players in the message
    message = addPlayerTags(message);

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

      // Don't send to the receiver if they ignore the sender
      if (ignores.getChatIgnore(receiver, sender))
        continue;

      // Override the default message
      receiver.sendMessage(
        cfg.get(ConfigKey.CHAT_MESSAGE_FORMAT)
          .withVariable("name", sender.getName())
          .withVariable("message", message)
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

  @Override
  public String translateColors(Player p, String message, String prefix) {
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
      if (!p.hasPermission(prefix + "." + color.name().toLowerCase()))
        continue;

      // Substitute color character to enable this color
      res.deleteCharAt(i);
      res.append("§");
    }

    return res.toString();
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler(priority = EventPriority.MONITOR)
  public void onChat(AsyncPlayerChatEvent e) {
    Player p = e.getPlayer();

    if (e.isCancelled())
      return;

    // Cancel the vanilla event
    e.setCancelled(true);

    if (mutes != null) {

      MuteModel mute = mutes.isCurrentlyMuted(p).orElse(null);

      // Check if the user is muted
      if (mute != null) {
        p.sendMessage(mutes.buildMuteScreen(mute));
        return;
      }
    }

    // Send using custom formatting
    sendChatMessage(p, new ArrayList<>(e.getRecipients()), e.getMessage());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Adds tags to player-names which are currently online
   * @param message Message which may contain player names
   * @return Message with substitutions
   */
  private String addPlayerTags(String message) {
    // Get a list of available names, longest names first
    // to replace greedily
    List<String> names = Bukkit.getOnlinePlayers()
      .stream()
      .map(Player::getName)
      .sorted((a, b) -> b.length() - a.length())
      .toList();

    // Keep track of color codes the player defined in the message here
    StringBuilder colors = new StringBuilder();

    // Get the shortest name to know at what offset to stop
    int shortestLen = names.get(names.size() - 1).length();

    // Loop till' end of message (minus shortest name)
    int offs = 0;
    while (offs <= message.length() - shortestLen) {

      // Check if any of the available names starts at this offset
      boolean anyMatched = false;
      for (String name : names) {

        // Name wouldn't fit, no need to check
        if (name.length() + offs > message.length())
          continue;

        // Check if the current name starts at the current offset
        boolean is = true;
        for (int i = offs; i < offs + name.length(); i++) {
          if (i != 0 && message.charAt(i - 1) == '§')
            colors.append("§").append(message.charAt(i));

          if (message.charAt(i) != name.charAt(i - offs)) {
            is = false;
            break;
          }
        }

        if (is) {
          // Make sure to restore colors after the tag
          String tag = cfg.get(ConfigKey.CHAT_TAG_FORMAT)
            .withVariable("name", name)
            .asScalar() + colors;

          // Substitute the name for the tag
          message = message.substring(0, offs) + tag + message.substring(offs + name.length());

          // Pick up right after the substituted in tag
          offs += tag.length();
          anyMatched = true;
          break;
        }
      }

      // Couldn't match any name at this offset, advance a character
      if (!anyMatched)
        offs++;
    }

    return message;
  }
}
