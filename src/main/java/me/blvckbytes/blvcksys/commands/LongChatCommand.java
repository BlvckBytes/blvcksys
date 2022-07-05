package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.events.IChatListener;
import me.blvckbytes.blvcksys.packets.communicators.bookeditor.IBookEditorCommunicator;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Send a really long text message into the chat by opening up a book view
  to type out all the text and then send it by closing the book.
*/
@AutoConstruct
public class LongChatCommand extends APlayerCommand {

  // Maximum length a long chat message is allowed to have
  private static final int MAX_LEN = 1024;

  private final IBookEditorCommunicator bookEditor;
  private final IChatListener chat;
  private final ChatUtil chatUtil;

  public LongChatCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IChatListener chat,
    @AutoInject ChatUtil chatUtil,
    @AutoInject IBookEditorCommunicator bookEditor
  ) {
    super(
      plugin, logger, cfg, refl,
      "longchat,lc",
      "Send a really long chat message",
      PlayerPermission.COMMAND_LONGCHAT.toString()
    );

    this.chat = chat;
    this.bookEditor = bookEditor;
    this.chatUtil = chatUtil;
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    handleEditor(p, new ArrayList<>());
  }

  /**
   * Handle the editor with it's max-length check, cancel, edit and send functionality
   * @param p Player to display for
   * @param pageContents Contents to already have in the book
   */
  private void handleEditor(Player p, List<String> pageContents) {
    // Open a new book editor containing the provided page contents
    if (bookEditor.initBookEditor(p, pageContents, pages -> {

      // Cancelled the request
      if (pages == null) {
        p.sendMessage(
          cfg.get(ConfigKey.LONGCHAT_CANCELLED)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      handleSubmit(p, pages);
    })) {
      // Notify about how it works
      p.sendMessage(
        cfg.get(ConfigKey.LONGCHAT_INIT)
          .withPrefix()
          .asScalar()
      );
    }
  }

  /**
   * Send a button prompt that can edit, cancel or (depending) send the
   * message and provide a preview if the message is ready to be sent
   * @param p Target player
   * @param pages Pages written, used to populate an edit book-request
   */
  private void handleSubmit(Player p, List<String> pages) {

    // Check whether this message can be sent by this player
    String message = joinPages(pages);
    boolean canSend = message.length() <= MAX_LEN || PlayerPermission.COMMAND_LONGCHAT_LIMITLESS.has(p);

    List<Triple<ConfigValue, @Nullable ConfigValue, Runnable>> buttons = new ArrayList<>();

    // Send this message
    if (canSend) {
      buttons.add(new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_YES), null, () -> {
        chat.sendChatMessage(p, Bukkit.getOnlinePlayers(), message);
      }));

      // Send message preview
      p.sendMessage(
        cfg.get(ConfigKey.LONGCHAT_PREVIEW)
          .withPrefix()
          .withVariable("message", message)
          .asScalar()
      );
    }

    // Edit this message
    buttons.add(new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_EDIT), null, () -> {
      handleEditor(p, pages);
    }));

    // Cancel sending
    buttons.add(new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_CANCEL), null, () -> {
      p.sendMessage(
        cfg.get(ConfigKey.LONGCHAT_CANCELLED)
          .withPrefix()
          .asScalar()
      );
    }));

    chatUtil.beginPrompt(
      p, null,
      cfg.get(canSend ? ConfigKey.LONGCHAT_CONFIRM : ConfigKey.LONGCHAT_LENGTH_EXCEEDED)
        .withPrefix()
        .withVariable("max_len", MAX_LEN),
      cfg.get(ConfigKey.CHATBUTTONS_EXPIRED),
      buttons
    );
  }

  /**
   * Join all pages of a book into a single string
   * @param pages Pages of the book
   * @return Joined string
   */
  private String joinPages(List<String> pages) {
    StringBuilder message = new StringBuilder();

    // Build the message from all individual pages
    for (String page : pages) {
      // Replace all newlines (join with space)
      page = page.replace("\n", " ").replace("\r", "");
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

    return message.toString().trim();
  }
}
