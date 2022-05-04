package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 04/28/2022

  Quickly display all available chat colors.
 */
@AutoConstruct
public class ColorCommand extends APlayerCommand {

  private String colors;

  public ColorCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "color",
      "Show all available chat colors",
      null
    );

    buildColors();
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    p.sendMessage(
      cfg.get(ConfigKey.COLOR_LISTING)
        .withPrefix()
        .withVariable("colors", colors)
        .asScalar()
    );
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Pre-build the string showcasing available colors
   */
  private void buildColors() {
    StringBuilder sb = new StringBuilder("§r");

    for (ChatColor cc : ChatColor.values()) {
      char c = cc.getChar();
      sb.append('&').append(c).append('§').append(c).append(c).append("§r ");
    }

    this.colors = sb.toString().trim();
  }
}
