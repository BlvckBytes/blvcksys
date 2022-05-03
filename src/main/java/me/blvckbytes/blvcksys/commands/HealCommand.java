package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.handlers.IObjectiveHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Heal yourself or an other player instantly.
 */
@AutoConstruct
public class HealCommand extends APlayerCommand {

  private final IObjectiveHandler obj;

  public HealCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IObjectiveHandler obj
  ) {
    super(
      plugin, logger, cfg, refl,
      "heal",
      "Heal yourself or others",
      PlayerPermission.HEAL,
      new CommandArgument("[player]", "The player to heal", PlayerPermission.HEAL_OTHERS)
    );

    this.obj = obj;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest online players
    if (currArg == 0)
      return suggestOnlinePlayers(p, args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0, p);
    boolean isSelf = target.equals(p);

    // Figure out what's the target's max health
    AttributeInstance maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
    double after = maxHealth == null ? 20.00 : maxHealth.getDefaultValue();
    double before = target.getHealth();

    // Calculate the delta
    double delta = Math.floor((after - before) * 100) / 100;
    String deltaStr = (delta >= 0 ? "+" : "") + delta;

    // Apply the health change (and update the scores)
    target.setHealth(after);
    this.obj.updateBelowName(target);

    // Inform target
    target.sendMessage(
      cfg.get(isSelf ? ConfigKey.HEAL_SELF : ConfigKey.HEAL_OTHERS_RECEIVER)
        .withPrefix()
        .withVariable("issuer", p.getName())
        .withVariable("delta", deltaStr)
        .asScalar()
    );

    // Inform sender
    if (!isSelf)
      p.sendMessage(
        cfg.get(ConfigKey.HEAL_OTHERS_SENDER)
          .withPrefix()
          .withVariable("target", target.getName())
          .withVariable("delta", deltaStr)
          .asScalar()
      );
  }
}
