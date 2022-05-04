package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.handlers.IObjectiveHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Manage a player's experience by setting (overwriting), adding, removing
  or getting (reading) their levels.
 */
@AutoConstruct
public class LevelCommand extends APlayerCommand {

  /**
   * Represents the action an executor can perform on a target's levels
   */
  private enum LevelAction {
    GET,      // Get the current number of levels
    GIVE,     // Give levels (adds)
    REMOVE,   // Remove levels (subtracts)
    SET       // Sets levels
  }

  private final IObjectiveHandler obj;

  public LevelCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IObjectiveHandler obj
  ) {
    super(
      plugin, logger, cfg, refl,
      "level",
      "Manage a player's experience",
      PlayerPermission.LEVEL,
      new CommandArgument("<action>", "Action to perform"),
      new CommandArgument("[player]", "Target player"),
      new CommandArgument("[amount]", "Amount of levels")
    );

    this.obj = obj;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest actions
    if (currArg == 0)
      return suggestEnum(args, currArg, LevelAction.class);

    // Suggest online players
    if (currArg == 1)
      return suggestOnlinePlayers(p, args, currArg, false);

    // Suggest placeholder
    if (currArg == 2)
      return Stream.of(getArgumentPlaceholder(currArg));

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    LevelAction action = parseEnum(LevelAction.class, args, 0, null);
    Player target = onlinePlayer(args, 1);
    boolean isSelf = target.equals(p);

    // Foreign target (not self)
    if (!isSelf)
      ensurePermission(p, PlayerPermission.LEVEL_OTHERS);

    // Get - read the current number of level
    if (action == LevelAction.GET) {
      p.sendMessage(
        cfg.get(isSelf ? ConfigKey.LEVELS_GET_SELF : ConfigKey.LEVELS_GET_OTHERS)
          .withPrefix()
          .withVariable("target", target.getName())
          .withVariable("level", target.getLevel())
          .asScalar()
      );
      return;
    }

    // Set, remove, give: Override, subtract or add a given amount of level
    if (
      action == LevelAction.SET ||
      action == LevelAction.REMOVE ||
      action == LevelAction.GIVE
    ) {
      int before = target.getLevel();
      int amount = parseInt(args, 2);

      // Decide on how to affect the amount of levels
      int after = before;
      switch (action) {
        case SET -> after = amount;
        case GIVE -> after += amount;
        case REMOVE -> after = Math.max(0, after - amount);
      }

      // Format a delta string
      int delta = after - before;
      String deltaStr = (delta >= 0 ? "+" : "") + delta;

      // Apply the change
      target.setLevel(after);
      obj.updateBelowName(target);

      // Notify the issuer
      p.sendMessage(
        cfg.get(isSelf ? ConfigKey.LEVELS_SET_SELF : ConfigKey.LEVELS_SET_OTHERS_SENDER)
          .withPrefix()
          .withVariable("target", target.getName())
          .withVariable("level", after)
          .withVariable("delta", deltaStr)
          .asScalar()
      );

      // Not self, notify the receiver
      if (!isSelf)
        target.sendMessage(
          cfg.get(ConfigKey.LEVELS_SET_OTHERS_RECEIVER)
            .withPrefix()
            .withVariable("issuer", p.getName())
            .withVariable("level", after)
            .withVariable("delta", deltaStr)
            .asScalar()
        );
    }
  }
}
