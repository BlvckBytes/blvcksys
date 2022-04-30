package me.blvckbytes.blvcksys.util;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Build a list of buttons, each having an action, text and a temporary command.
  Then, call build to generate a new Component containing all buttons, to be sent
  to a player. Buttons can be invoked using their temporary command by a routine.
  Callback invocations are synchronized with the main thread.
*/
public class ChatButtons {

  /**
   * Represents a button that can be clicked within the chat, which
   * executes it's assigned temporary command
   */
  @AllArgsConstructor
  private static class CommandButton {
    private String tempCommand;
    private String text;
    private Runnable action;
  }

  // Each player may have multiple buttons to choose from
  private final List<CommandButton> buttons;

  private final IConfig cfg;
  private final boolean clickOnce;
  private final JavaPlugin plugin;
  private final String prefix;

  /**
   * Create a new button builder/handler
   * @param plugin Plugin reference
   * @param cfg Config reference
   * @param clickOnce Whether to expire all buttons when one has been clicked
   */
  public ChatButtons(String prefix, boolean clickOnce, JavaPlugin plugin, IConfig cfg) {
    this.cfg = cfg;
    this.prefix = prefix;
    this.clickOnce = clickOnce;
    this.plugin = plugin;
    this.buttons = new ArrayList<>();
  }

  /**
   * Add a new button to the set of buttons
   * @param key Key for the button's template
   * @param action Action to run on click
   */
  public ChatButtons addButton(ConfigKey key, Runnable action) {
    // Generate a new, random command name
    String tmpCmd = UUID.randomUUID().toString();

    // Register this button
    this.buttons.add(
      new CommandButton(tmpCmd, cfg.get(key).asScalar(), action)
    );

    return this;
  }

  /**
   * Build a new component to be sent out to recipients
   * @return Built component
   */
  public TextComponent buildComponent() {
    TextComponent head = new TextComponent(prefix);

    boolean firstIter = true;
    for (CommandButton button : this.buttons) {
      // Create the component from it's displayed text, space buttons out
      TextComponent btn = new TextComponent((firstIter ? "" : " ") + button.text);

      // Bind the temporary command to it's click listener
      btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + button.tempCommand));

      // Bind a hover event for info purposes
      btn.setHoverEvent(new HoverEvent(
        HoverEvent.Action.SHOW_TEXT,
        new Text(cfg.get(ConfigKey.CHATBUTTONS_HOVER).asScalar()
      )));

      // Append to head
      head.addExtra(btn);
      firstIter = false;
    }

    return head;
  }

  /**
   * Process the invocation of a dispatched command within this instance
   * @param command Dispatched command without the leading /
   * @return True if a button has been used, false on no matches
   */
  public boolean processInvocation(String command) {
    for (int i = this.buttons.size() - 1; i >= 0; i--) {
      CommandButton button = this.buttons.get(i);

      // The command doesn't target this button
      if (!button.tempCommand.equals(command))
        continue;

      // Call the callback
      Bukkit.getScheduler().runTask(this.plugin, () -> button.action.run());

      // Remove the button or kill all buttons
      if (clickOnce)
        this.buttons.clear();
      else
        this.buttons.remove(i);

      return true;
    }

    // No button corresponds to this command
    return false;
  }

  /**
   * Build an instance that contains YES/NO buttons
   * @param prefix Prefix to prepend to the buttons
   * @param plugin Plugin reference
   * @param cfg Config reference
   * @param yes Called when clicking YES
   * @param no Called when clicking NO
   * @return ChatButtons instance
   */
  public static ChatButtons buildYesNo(String prefix, JavaPlugin plugin, IConfig cfg, Runnable yes, Runnable no) {
    return new ChatButtons(prefix, true, plugin, cfg)
      .addButton(ConfigKey.CHATBUTTONS_YES, yes)
      .addButton(ConfigKey.CHATBUTTONS_NO, no);
  }
}
