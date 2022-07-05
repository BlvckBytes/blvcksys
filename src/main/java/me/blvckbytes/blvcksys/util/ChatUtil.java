package me.blvckbytes.blvcksys.util;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
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

  @AllArgsConstructor
  public static class ChatPrompt {
    @Nullable Consumer<String> chat;
    Map<String, Runnable> actions;
    @Nullable ConfigValue expiredMessage;
    boolean expired;
  }

  private final Map<Player, List<ChatPrompt>> chatPrompts;

  public ChatUtil() {
    this.chatPrompts = new HashMap<>();
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  /**
   * Begin a new prompt session for a given player
   * @param p Target player
   * @param chat Chat message listener, null if no chat message is required
   * @param prepend Message to prepend to the buttons
   * @param expired Message to print if the prompt already expired
   * @param actions Action buttons to append to the message (text, hover, action)
   * @return Prompt handle
   */
  public ChatPrompt beginPrompt(
    Player p,
    @Nullable Consumer<String> chat,
    ConfigValue prepend,
    @Nullable ConfigValue expired,
    @Nullable List<Triple<ConfigValue, @Nullable ConfigValue, Runnable>> actions
  ) {
    TextComponent head = new TextComponent(prepend.asScalar());
    Map<String, Runnable> actionButtons = new HashMap<>();

    if (actions == null)
      actions = new ArrayList<>();

    for (Triple<ConfigValue, @Nullable ConfigValue, Runnable> action : actions) {
      // Create the component from it's displayed text, space buttons out
      TextComponent btn = new TextComponent(" " + action.a().asScalar());
      String actionCommand = UUID.randomUUID().toString();

      // Bind the temporary command to it's click listener
      btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + actionCommand));

      // Show hover text, if provided
      if (action.b() != null)
        btn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(action.b().asScalar())));

      // Append to head
      head.addExtra(btn);

      actionButtons.put(actionCommand, action.c());
    }

    // Register the prompt
    if (!chatPrompts.containsKey(p))
      chatPrompts.put(p, new ArrayList<>());

    ChatPrompt prompt = new ChatPrompt(chat, actionButtons, expired, false);
    chatPrompts.get(p).add(prompt);

    // Send out the prompt component
    p.spigot().sendMessage(head);
    return prompt;
  }

  /**
   * Expire a previously started prompt
   * @param prompt Prompt handle
   */
  public void expirePrompt(ChatPrompt prompt) {
    prompt.expired = true;
  }

  /**
   * Checks whether the given player has any active (pending) prompts
   * @param p Target player
   */
  public boolean hasActivePrompt(Player p) {
    return chatPrompts.getOrDefault(p, new ArrayList<>()).stream().anyMatch(prompt -> !prompt.expired);
  }

  //=========================================================================//
  //                                 Listener                                //
  //=========================================================================//

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

    // Has no active prompts yet
    List<ChatPrompt> prompts = chatPrompts.get(p);
    if (prompts == null)
      return;

    // Search target prompt by command string
    ChatPrompt target = prompts.stream()
      .filter(prompt -> prompt.actions.containsKey(command))
      .findFirst()
      .orElse(null);

    // Command didn't target an existing prompt
    if (target == null)
      return;

    // Prompt already expired
    if (target.expired) {
      if (target.expiredMessage != null) {
        p.sendMessage(
          target.expiredMessage
            .withPrefix()
            .asScalar()
        );
      }
      return;
    }

    // Dispatch the callback and expire the prompt
    target.actions.get(command).run();
    target.expired = true;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(AsyncPlayerChatEvent e) {
    String message = e.getMessage();
    Player p = e.getPlayer();

    // Has no active prompts yet
    List<ChatPrompt> prompts = chatPrompts.get(p);
    if (prompts == null)
      return;

    // Search the first non-expired chat prompt
    ChatPrompt target = prompts.stream()
      .filter(prompt -> !prompt.expired && prompt.chat != null)
      .findFirst().orElse(null);

    // No pending prompt remaining
    if (target == null || target.chat == null)
      return;

    // Dispatch the callback and expire the prompt
    e.setCancelled(true);
    target.expired = true;
    target.chat.accept(message);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Unregister all prompts on quit
    chatPrompts.remove(e.getPlayer());
  }
}
