package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.events.IChatListener;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Kick a specific player or all online players from the server.
*/
@AutoConstruct
public class KickCommand extends APlayerCommand {

  private final IChatListener chat;

  public KickCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IChatListener chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "kick",
      "Kick a player from the server",
      PlayerPermission.COMMAND_KICK.toString(),
      new CommandArgument("<player/all>", "Name of the player to kick"),
      new CommandArgument("[Reason]", "Reason of the kick")
    );

    this.chat = chat;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOnlinePlayers(p, args, currArg, true);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    boolean all = argval(args, 0).equalsIgnoreCase("all");
    String reason = argvar(args, 1, cfg.get(ConfigKey.KICK_DEFAULT_REASON).asScalar());

    // Allow for custom colored reasons
    reason = ChatColor.translateAlternateColorCodes('&', reason);

    if (all) {
      // Kick all players
      for (Player target : Bukkit.getOnlinePlayers()) {
        // Skip self
        if (target == p)
          continue;

        // Not affected by kickalls
        if (PlayerPermission.COMMAND_KICK_KICKALL_BYPASS.has(target))
          continue;

        kickPlayer(p, target, reason);
      }

      // Notify the executor
      p.sendMessage(
        cfg.get(ConfigKey.KICK_KICKED_ALL)
          .withPrefix()
          .withVariable("issuer", p.getName())
          .withVariable("reason", reason)
          .asScalar()
      );
      return;
    }

    // Kick the specific player
    Player target = onlinePlayer(args, 0);

    // Cannot kick yourself
    if (target == p) {
      p.sendMessage(
        cfg.get(ConfigKey.KICK_TRIEDSELF)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // This player cannot be kicked and the sender doesn't have
    // the override permission
    if (
      PlayerPermission.COMMAND_KICK_UNKICKABLE.has(target) &&
      !PlayerPermission.COMMAND_KICK_UNKICKABLE_OVERRIDE.has(p)
    ) {
      p.sendMessage(
        cfg.get(ConfigKey.KICK_UNKICKABLE)
          .withPrefix()
          .withVariable("target", target.getName())
          .asScalar()
      );
      return;
    }

    kickPlayer(p, target, reason);

    // Broadcast this kick
    chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.KICK_KICKED_TARGET)
        .withPrefixes()
        .withVariable("issuer", p.getName())
        .withVariable("target", target.getName())
        .withVariable("reason", reason)
        .asScalar()
    );
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Kick a player from the server and display the kick-screen to them
   * @param issuer Action issuer
   * @param target Kick target
   * @param reason Reason for this kick
   */
  private void kickPlayer(Player issuer, Player target, String reason) {
    target.kickPlayer(
      cfg.get(ConfigKey.KICK_SCREEN)
        .withVariable("reason", reason)
        .withVariable("issuer", issuer.getName())
        .asScalar()
    );
  }
}
