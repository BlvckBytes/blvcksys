package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.INpcHandler;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.NpcModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/20/2022

  Create, delete, move and change the skin of fake npcs.
*/
@AutoConstruct
public class NpcCommand extends APlayerCommand {

  private enum NpcAction {
    CREATE,
    DELETE,
    MOVEHERE,
    SETSKIN
  }

  private final INpcHandler npcs;
  private final IPersistence pers;

  public NpcCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject INpcHandler npcs,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "npc",
      "Manage fake NPCs",
      PlayerPermission.COMMAND_NPC,
      new CommandArgument("<name>", "Name of the npc"),
      new CommandArgument("<action>", "Action to perform"),
      new CommandArgument("[skin]", "Skin value")
    );

    this.npcs = npcs;
    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestModels(args, currArg, NpcModel.class, "name", pers);

    if (currArg == 1)
      return suggestEnum(args, currArg, NpcAction.class);

    if (currArg == 2)
      return Stream.of(getArgumentPlaceholder(currArg));

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    NpcAction action = parseEnum(NpcAction.class, args, 1, null);

    if (action == NpcAction.CREATE) {
      Optional<NpcModel> npc = npcs.createNpc(p, name, p.getLocation());

      if (npc.isEmpty()) {
        p.sendMessage(
          cfg.get(ConfigKey.NPC_EXISTS)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.NPC_CREATED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == NpcAction.DELETE) {

      if (!npcs.deleteNpc(name)) {
        p.sendMessage(
          cfg.get(ConfigKey.NPC_NOT_FOUND)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.NPC_DELETED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == NpcAction.MOVEHERE) {
      if (!npcs.moveNpc(name, p.getLocation())) {
        p.sendMessage(
          cfg.get(ConfigKey.NPC_NOT_FOUND)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.NPC_MOVED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == NpcAction.SETSKIN) {
      String skin = argval(args, 2);
      TriResult res = npcs.changeSkin(name, skin);

      if (res == TriResult.EMPTY) {
        p.sendMessage(
          cfg.get(ConfigKey.NPC_NOT_FOUND)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      if (res == TriResult.ERR) {
        p.sendMessage(
          cfg.get(ConfigKey.NPC_SKIN_NOT_LOADABLE)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.NPC_SKIN_CHANGED)
          .withPrefix()
          .withVariable("name", name)
          .withVariable("skin", skin)
          .asScalar()
      );
      return;
    }
  }
}
