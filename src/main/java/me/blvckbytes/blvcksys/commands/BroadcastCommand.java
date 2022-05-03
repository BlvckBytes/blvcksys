package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.events.IChatListener;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/03/2022

  Broadcast a message to all players which are currently online.
*/
@AutoConstruct
public class BroadcastCommand extends APlayerCommand {

  private final IChatListener chat;

  public BroadcastCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IChatListener chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "broadcast,bc",
      "Broadcast a message to all players",
      PlayerPermission.COMMAND_BROADCAST,
      new CommandArgument("<message>", "The message to broadcast")
    );

    this.chat = chat;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String message = cfg.get(ConfigKey.BROADCAST_FORMAT)
      // Allow for colorful broadcasts
      .withVariable("message", ChatColor.translateAlternateColorCodes('&', argvar(args, 0)))
      .asScalar();

    String hover = cfg.get(ConfigKey.BROADCAST_HOVER)
      .withVariable("issuer", p.getName())
      .asScalar();

    TextComponent comp = new TextComponent(message);

    // Create a hoverable message if the hover text is set
    if (!hover.isEmpty())
      comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));

    // Send this message out to all recipients
    chat.broadcastMessage(Bukkit.getOnlinePlayers(), comp);
  }
}
