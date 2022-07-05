package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.INpcHandler;
import me.blvckbytes.blvcksys.handlers.ITeleportationHandler;
import me.blvckbytes.blvcksys.persistence.models.NpcModel;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/20/2022

  List all global npcs or just npcs near you.
*/
@AutoConstruct
public class NpcsCommand extends APlayerCommand {

  // What radius to use as a default when no arg has been specified
  private static final float RADIUS_FALLBACK = 50;

  private final INpcHandler npcs;
  private final ChatUtil chat;
  private final ITeleportationHandler tp;

  public NpcsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject INpcHandler npcs,
    @AutoInject ChatUtil chat,
    @AutoInject ITeleportationHandler tp
  ) {
    super(
      plugin, logger, cfg, refl,
      "npcs",
      "List nearby npcs",
      PlayerPermission.COMMAND_NPC.toString(),
      new CommandArgument("[radius]", "Radius to list within")
    );

    this.npcs = npcs;
    this.chat = chat;
    this.tp = tp;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest radius placeholder
    if (currArg == 0)
      return Stream.of(getArgumentPlaceholder(currArg));

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    float radius = parseFloat(args, 0, RADIUS_FALLBACK);

    List<NpcModel> npcs = this.npcs.getNear(p.getLocation(), radius);

    // No npcs near the player
    if (npcs.size() == 0) {
      p.sendMessage(
        cfg.get(ConfigKey.NPC_LIST_PREFIX)
          .withPrefix()
          .withVariable("radius", radius)
          .asScalar() +
        cfg.get(ConfigKey.NPC_LIST_NONE)
          .asScalar()
      );
      return;
    }

    // Add all npc to the list
    List<Triple<ConfigValue, @Nullable ConfigValue, Runnable>> buttons = new ArrayList<>();
    for (int i = 0; i < npcs.size(); i++) {
      NpcModel npc = npcs.get(i);

      // Display the location of the first line
      Location l = npc.getLoc();

      buttons.add(new Triple<>(
        cfg.get(ConfigKey.NPC_LIST_FORMAT)
          .withVariable("name", npc.getName())
          .withVariable("sep", i == npcs.size() - 1 ? "" : ", "),
        cfg.get(ConfigKey.NPC_LIST_HOVER_TEXT)
          .withVariable("created_at", npc.getCreatedAtStr())
          .withVariable("updated_at", npc.getUpdatedAtStr())
          .withVariable("creator", npc.getCreator().getName())
          .withVariable("skin", npc.getSkinOwnerName() == null ? "/" : npc.getSkinOwnerName())
          .withVariable("distance", (int) npc.getLoc().distance(p.getLocation()))
          .withVariable("location", "(" + l.getBlockX() + " | " + l.getBlockY() + " | " + l.getBlockZ() + ")"),
        () -> {
          tp.requestTeleportation(p, l, () -> {
            p.sendMessage(
              cfg.get(ConfigKey.NPC_LIST_TELEPORTED)
                .withPrefix()
                .withVariable("name", npc.getName())
                .asScalar()
            );
          }, null);
        }
      ));
    }

    chat.beginPrompt(
      p, null,
      cfg.get(ConfigKey.NPC_LIST_PREFIX)
        .withPrefix()
        .withVariable("radius", radius),
      cfg.get(ConfigKey.CHATBUTTONS_EXPIRED),
      buttons
    );
  }
}
