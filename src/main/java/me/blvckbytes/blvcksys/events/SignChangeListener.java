package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Listens for sign changes (basically creations) and substitutes
  colors as the player's permissions allow for.
*/
@AutoConstruct
public class SignChangeListener implements Listener {

  private final IChatListener chat;

  public SignChangeListener(
    @AutoInject IChatListener chat
  ) {
    this.chat = chat;
  }

  @EventHandler
  public void onSignChange(SignChangeEvent e) {
    String[] lines = e.getLines();

    // Translate all sign colors
    for (int i = 0; i < lines.length; i++)
      e.setLine(i, chat.translateColors(e.getPlayer(), lines[i], PlayerPermission.SIGN_COLOR_PREFIX));
  }
}
