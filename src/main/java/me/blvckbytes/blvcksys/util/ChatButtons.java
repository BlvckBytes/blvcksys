package me.blvckbytes.blvcksys.util;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

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
  private final Function<Exception, String> exceptionMapper;

  /**
   * Create a new button builder/handler
   * @param plugin Plugin reference
   * @param cfg Config reference
   * @param clickOnce Whether to expire all buttons when one has been clicked
   * @param exceptionMapper Used to decide what message to display to the player on
   *                        exceptions, nullable to just send out an internal error response
   */
  public ChatButtons(
    String prefix,
    boolean clickOnce,
    JavaPlugin plugin,
    IConfig cfg,
    @Nullable Function<Exception, String> exceptionMapper
  ) {
    this.cfg = cfg;
    this.prefix = prefix;
    this.clickOnce = clickOnce;
    this.plugin = plugin;
    this.exceptionMapper = exceptionMapper;

    this.buttons = new ArrayList<>();
  }

  /**
   * Add a new button to the set of buttons
   * @param cfgVal Value for the button's template
   * @param action Action to run on click
   */
  public ChatButtons addButton(ConfigValue cfgVal, Runnable action) {
    // Generate a new, random command name
    String tmpCmd = UUID.randomUUID().toString();

    // Register this button
    this.buttons.add(
      new CommandButton(tmpCmd, cfgVal.asScalar(), action)
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

      // Append to head
      head.addExtra(btn);
      firstIter = false;
    }

    return head;
  }

  /**
   * Process the invocation of a dispatched command within this instance
   * @param p Player that invoked this command, used for error reporting
   * @param command Dispatched command without the leading /
   * @return True if a button has been used, false on no matches
   */
  public boolean processInvocation(Player p, String command) {
    for (int i = this.buttons.size() - 1; i >= 0; i--) {
      CommandButton button = this.buttons.get(i);

      // The command doesn't target this button
      if (!button.tempCommand.equals(command))
        continue;

      // Call the callback
      Bukkit.getScheduler().runTask(this.plugin, () -> {
        try {
          button.action.run();
        } catch (Exception e) {
          if (this.exceptionMapper == null) {
            p.sendMessage(
              cfg.get(ConfigKey.ERR_INTERNAL)
                .withPrefix()
                .asScalar()
            );

            return;
          }

          p.sendMessage(this.exceptionMapper.apply(e));
        }
      });

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
  public static ChatButtons buildYesNo(
    String prefix,
    JavaPlugin plugin,
    IConfig cfg,
    Runnable yes,
    Runnable no,
    @Nullable Function<Exception, String> exceptionMapper
  ) {
    return new ChatButtons(prefix, true, plugin, cfg, exceptionMapper)
      .addButton(cfg.get(ConfigKey.CHATBUTTONS_YES), yes)
      .addButton(cfg.get(ConfigKey.CHATBUTTONS_NO), no);
  }

  /**
   * Build a simple button without a prefix or any exception mapping
   * @param text Text to display
   * @param plugin Plugin reference
   * @param cfg Config reference
   * @param clicked Called when clicked
   * @return ChatButtons instance
   */
  public static ChatButtons buildSimple(
    ConfigValue text,
    JavaPlugin plugin,
    IConfig cfg,
    Runnable clicked
  ) {
    return new ChatButtons("", true, plugin, cfg, null)
      .addButton(text, clicked);
  }
}
