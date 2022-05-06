package me.blvckbytes.blvcksys.util;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Utility methods that targets the chat and messages sent to it.
*/
@AutoConstruct
public class ChatUtil implements Listener {

  // Pattern to check if a command matches the UUID format (and thus is a temporary command)
  private static final Pattern UUID_PATTERN = Pattern.compile("[a-f\\d]{8}(?:-[a-f\\d]{4}){4}[a-f\\d]{8}");

  // A player can have multiple sessions of buttons to choose from
  private final Map<Player, List<ChatButtons>> buttonSessions;

  private final IConfig cfg;

  public ChatUtil(
    @AutoInject IConfig cfg
  ) {
    this.cfg = cfg;
    this.buttonSessions = new HashMap<>();
  }

  /**
   * Register and send buttons to a player
   * @param p Target player
   * @param btns Previously built set of buttons
   */
  public void sendButtons(Player p, ChatButtons btns) {
    // Register this instance of buttons
    if (!buttonSessions.containsKey(p))
      buttonSessions.put(p, new ArrayList<>());
    buttonSessions.get(p).add(btns);

    // Send the buttons to the player
    p.spigot().sendMessage(btns.buildComponent());
  }

  /**
   * Remove a previously added set of buttons again
   * @param p Target player
   * @param btns Previously added set of buttons
   */
  public void removeButtons(Player p, ChatButtons btns) {
    // Player not even known
    if (!buttonSessions.containsKey(p))
      return;

    // Remove element
    buttonSessions.get(p).remove(btns);
  }

  /**
   * Clear all sessions of a player
   * @param p Target player
   */
  public void clearPlayer(Player p) {
    buttonSessions.remove(p);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPreCommand(PlayerCommandPreprocessEvent e) {
    String message = e.getMessage();
    Player p = e.getPlayer();

    // Not a unique command
    if (message.length() == 0 || message.contains(" "))
      return;

    String command = message.substring(1);

    // Command needs to be a unique ID
    if (!UUID_PATTERN.matcher(command).matches())
      return;

    // Cancel all commands that are UUIDs - they're only internal
    e.setCancelled(true);

    // Try to find the target button and dispatch it
    List<ChatButtons> sessions = buttonSessions.get(p);
    if (sessions != null) {
      for (ChatButtons session : sessions) {
        if (session.processInvocation(p, command))
          return;
      }
    }

    // Inform about expiry
    p.sendMessage(
      cfg.get(ConfigKey.CHATBUTTONS_EXPIRED)
        .withPrefix()
        .asScalar()
    );
  }
}
