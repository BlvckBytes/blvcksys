package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHologramHandler;
import me.blvckbytes.blvcksys.persistence.models.HologramLineModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  List all global holograms or just holograms near you.
*/
@AutoConstruct
public class HolosCommand extends APlayerCommand {

  // What radius to use as a default when no arg has been specified
  private static final float RADIUS_FALLBACK = 50;

  private final IHologramHandler holo;

  public HolosCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IHologramHandler holo
  ) {
    super(
      plugin, logger, cfg, refl,
      "holos",
      "List nearby holograms",
      PlayerPermission.COMMAND_HOLO,
      new CommandArgument("[radius]", "Radius to list within")
    );

    this.holo = holo;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    float radius = parseFloat(args, 0, RADIUS_FALLBACK);

    // Get all near holograms and map them to a triple containing the
    // hologram name, the distance between the first line and the player
    // as well as all individual lines.
    List<Triple<String, Double, List<HologramLineModel>>> holograms = this.holo.getNear(p.getLocation(), radius)
      .entrySet().stream()
      .map(e -> {
        Location firstLoc = e.getValue().get(0).getLoc();
        return new Triple<>(e.getKey(), firstLoc.distance(p.getLocation()), e.getValue());
      })
      .sorted((a, b) -> (int) (a.b() - b.b()))
      .toList();

    // Begin the head of the component chain with the list prefix
    TextComponent res = new TextComponent(
      cfg.get(ConfigKey.COMMAND_HOLOS_LIST_PREFIX)
        .withPrefix()
        .withVariable("radius", radius)
        .asScalar()
    );

    // Add all holograms to the list
    for (int i = 0; i < holograms.size(); i++) {
      Triple<String, Double, List<HologramLineModel>> hologram = holograms.get(i);

      // Displayed text
      TextComponent holoComp = new TextComponent(
        cfg.get(ConfigKey.COMMAND_HOLOS_LIST_FORMAT)
          .withVariable("name", hologram.a())
          .asScalar()
          + (i == holograms.size() - 1 ? "" : ", ")
      );

      // Build the list of creators by making a unique list
      // from all line creators and joining all the names into a single string
      StringBuilder creatorsSb = new StringBuilder();
      List<String> creators = new ArrayList<>();

      // Also search for the first creation date and the last update date
      Date firstCreation = new Date();
      String firstCreationStr = "/";

      for (int j = 0; j < hologram.c().size(); j++) {
        HologramLineModel line = hologram.c().get(j);

        if (line.getCreatedAt() != null && firstCreation.after(line.getCreatedAt())) {
          firstCreation = line.getCreatedAt();
          firstCreationStr = line.getCreatedAtStr();
        }

        String name = line.getCreator().getName();
        if (creators.contains(name))
          continue;

        creators.add(name);
        creatorsSb.append(
          cfg.get(ConfigKey.COMMAND_HOLOS_LIST_HOVER_CREATORS_FORMAT)
            .withVariable("creator", name)
            .asScalar()
        ).append(j == hologram.c().size() - 1 ? "" : ", ");
      }

      // Display the location of the first line
      Location l = hologram.c().get(0).getLoc();

      // Text when hovering
      holoComp.setHoverEvent(new HoverEvent(
        HoverEvent.Action.SHOW_TEXT,
        new Text(
          cfg.get(ConfigKey.COMMAND_HOLOS_LIST_HOVER_TEXT)
            .withVariable("created_at", firstCreationStr)
            .withVariable("creators", creatorsSb.toString())
            .withVariable("num_lines", hologram.c().size())
            .withVariable("distance", hologram.b().intValue())
            .withVariable("location", "(" + l.getBlockX() + " | " + l.getBlockY() + " | " + l.getBlockZ() + ")")
            .asScalar()
        )
      ));

      res.addExtra(holoComp);
    }

    // No holograms near the player
    if (holograms.size() == 0) {
      res.addExtra(new TextComponent(
        cfg.get(ConfigKey.COMMAND_HOLOS_LIST_NONE)
          .asScalar()
      ));
    }

    p.spigot().sendMessage(res);
  }
}
