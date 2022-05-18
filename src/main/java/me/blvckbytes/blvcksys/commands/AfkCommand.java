package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.commands.exceptions.CooldownException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.events.IAfkListener;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.CooldownSessionModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Allows players to mark themselves as AFK on demand
*/
@AutoConstruct
public class AfkCommand extends APlayerCommand {

  // Cooldown token for the AFK cooldown
  private static final String CT_AFK = "cmd_afk";

  // Cooldown duration for the AFK command in seconds
  private static final int CD_AFK = 60;

  private final IAfkListener afk;
  private final IPersistence pers;

  public AfkCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IAfkListener afk,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "afk",
      "Mark yourself as AFK",
      null
    );

    this.afk = afk;
    this.pers = pers;
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    // Is already AFK
    if (afk.isAFK(p)) {
      p.sendMessage(
        cfg.get(ConfigKey.AFK_ALREADY)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    cooldownGuard(p, pers, CT_AFK, CD_AFK, PlayerPermission.AFK_COOLDOWN_BYPASS);

    afk.setAFK(p);
  }
}
