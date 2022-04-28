package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.ChatButtons;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  A command to quickly test a few lines of code during development sessions
*/
@AutoConstruct
public class TestCommand extends APlayerCommand {

  private final ChatUtil chat;

  public TestCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ChatUtil chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "test",
      "A command for development testing purposes only",
      null
    );

    this.chat = chat;
  }

  private int c = 0;

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    ChatButtons<Integer> buttons = new ChatButtons<>(
      cfg.get(ConfigKey.PREFIX).asScalar() + " §7Buttons: ", true, plugin, cfg, ++c
    )
      .addButton(ConfigKey.CHATBUTTONS_YES, (v) -> {
        p.sendMessage(v + " YES");
      })
      .addButton(ConfigKey.CHATBUTTONS_NO, (v) -> {
        p.sendMessage(v + " NO");
      });

      chat.sendButtons(p, buttons);
  }
}
