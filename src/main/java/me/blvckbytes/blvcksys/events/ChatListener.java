package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.packets.modifiers.tablist.ITabListModifier;
import me.blvckbytes.blvcksys.packets.modifiers.tablist.TabListGroup;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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
public class ChatListener implements Listener {

  private final IConfig cfg;
  private final ITabListModifier tab;

  public ChatListener(
    @AutoInject IConfig cfg,
    @AutoInject ITabListModifier tab
  ) {
    this.cfg = cfg;
    this.tab = tab;
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onChat(AsyncPlayerChatEvent e) {
    Player p = e.getPlayer();

    // Cancel the vanilla event
    e.setCancelled(true);

    Optional<TabListGroup> tg = tab.getPlayerGroup(p);
    String prefix = tg.map(TabListGroup::prefix).orElse("§r");

    for(Player recv : e.getRecipients()) {
      // Override the default message
      recv.sendMessage(
        cfg.get(ConfigKey.CHAT_FORMAT)
          .withVariable("name", p.getName())
          .withVariable("message", translateColors(p, e.getMessage()))
          .withVariable("prefix", prefix)
          .asScalar()
      );
    }
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Translate all colors in a string based on a player's permissions
   * @param p Target player
   * @param message String to translate
   * @return Color-translated string
   */
  private String translateColors(Player p, String message) {
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

      // Player cannot write using this color, skip
      if (!PlayerPermission.CHAT_COLOR_PREFIX.has(p, color.name().toLowerCase()))
        continue;

      // Substitute color character to enable this color
      res.deleteCharAt(i);
      res.append("§");
    }

    return res.toString();
  }
}
