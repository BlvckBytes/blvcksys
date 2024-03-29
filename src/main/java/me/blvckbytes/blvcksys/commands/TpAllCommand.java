package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Teleport all online players to your location.
 */
@AutoConstruct
public class TpAllCommand extends APlayerCommand {

  // Mapping players to their last button request in order to
  // invalidate old prompts when executing again
  private final Map<Player, ChatUtil.ChatPrompt> lastButtons;

  private final ChatUtil chat;

  public TpAllCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ChatUtil chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "tpall",
      "Teleport all players to you",
      PlayerPermission.COMMAND_TPALL.toString()
    );

    this.lastButtons = new HashMap<>();
    this.chat = chat;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    ChatUtil.ChatPrompt prompt = chat.beginPrompt(
      p, null,
      cfg.get(ConfigKey.TPALL_CONFIRMATION_PREFIX)
        .withPrefix(),
      cfg.get(ConfigKey.CHATBUTTONS_EXPIRED),
      List.of(
        new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_YES), null, () -> {
          for (Player t : Bukkit.getOnlinePlayers()) {
            // Send out the broadcast
            t.sendMessage(
              cfg.get(ConfigKey.TPALL_BROADCAST)
                .withPrefix()
                .withVariable("issuer", p.getName())
                .asScalar()
            );

            // Skip self
            if (t.equals(p))
              continue;

            // Teleport to the issuer
            t.teleport(p);
          }
        }),
        new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_NO), null, () -> {
          p.sendMessage(
            cfg.get(ConfigKey.TPALL_CANCELLED)
              .withPrefix()
              .asScalar()
          );
        })
      )
    );

    // Invalidate previous prompt, if existing
    ChatUtil.ChatPrompt prev = lastButtons.remove(p);
    if (prev != null)
      chat.expirePrompt(prev);

    // Save a ref to these buttons
    lastButtons.put(p, prompt);
  }
}
