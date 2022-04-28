package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.packets.communicators.bookeditor.IBookEditorCommunicator;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Send a really long text message into the chat by opening up a book view
  to type out all the text and then send it by closing the book.
*/
@AutoConstruct
public class LongChat extends APlayerCommand {

  private final IBookEditorCommunicator bookEditor;

  public LongChat(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IBookEditorCommunicator bookEditor
  ) {
    super(
      plugin, logger, cfg, refl,
      "longchat,lc",
      "Send a really long text message",
      PlayerPermission.LONGCHAT
    );

    this.bookEditor = bookEditor;
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    // Open a new empty book editor
    if (bookEditor.initBookEditor(p, new ArrayList<>(), writtenPages -> {

      // Cancelled the request
      if (writtenPages == null) {
        p.sendMessage(
          cfg.get(ConfigKey.LONGCHAT_CANCELLED)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      // Build the message from all individual pages
      StringBuilder message = new StringBuilder();
      for (String page : writtenPages) {
        // Replace all newlines
        page = page.replace("\n", "").replace("\r", "");
        char lastChar = message.length() == 0 ? 0 : message.charAt(message.length() - 1);

        // Last char of message ends with space and page starts with space, remove one of the two
        if (lastChar == ' ' && page.startsWith(" "))
          message.append(page.substring(1));

        // Neither one of them offers a space, insert one
        else if (lastChar != ' ' && !page.startsWith(" "))
          message.append(" ").append(page);

        // Join without modification
        else
          message.append(page);
      }

      // Call an artificially created chat event
      Bukkit.getPluginManager().callEvent(new AsyncPlayerChatEvent(
        false, p,
        message.toString(),
        new HashSet<>(Bukkit.getOnlinePlayers())
      ));
    })) {
      // Notify about how it works
      p.sendMessage(
        cfg.get(ConfigKey.LONGCHAT_INIT)
          .withPrefix()
          .asScalar()
      );
    }
  }
}
