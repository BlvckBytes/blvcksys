package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 04/28/2022

  Clear the chat for yourself or all players (globally).
 */
@AutoConstruct
public class ClearChatCommand extends APlayerCommand {

  // How many empty lines to send in order to clear a chat
  private static final int NUM_EMPTY_LINES = 200;

  public ClearChatCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "clearchat,cc",
      "Clear the chat for yourself or others",
      PlayerPermission.COMMAND_CLEARCHAT_SELF.toString(),
      new CommandArgument("[global]", "Whether to clear globally", PlayerPermission.COMMAND_CLEARCHAT_GLOBAL.toString())
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return Stream.of("global");
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String type = argval(args, 0, "self");

    if (type.equals("global"))
      for (Player t : Bukkit.getOnlinePlayers())
        clearChat(t, p, true);
    else
      clearChat(p, p, false);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Clear the chat for a specific player
   * @param p Player to clear
   * @param issuer Command issuer
   * @param isGlobal Whether or not this was a global clear
   */
  private void clearChat(Player p, Player issuer, boolean isGlobal) {
    // Send out empty lines
    for (int i = 0; i < NUM_EMPTY_LINES; i++)
      p.sendMessage(" ");

    // Inform about the clearing cause
    p.sendMessage(
      cfg.get(isGlobal ? ConfigKey.CLEARCHAT_GLOBAL : ConfigKey.CLEARCHAT_SELF)
        .withVariable("issuer", issuer.getName())
        .withPrefix()
        .asScalar()
    );
  }
}
