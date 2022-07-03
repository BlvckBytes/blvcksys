package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.ICrateHandler;
import me.blvckbytes.blvcksys.handlers.ITeleportationHandler;
import me.blvckbytes.blvcksys.persistence.models.CrateItemModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import me.blvckbytes.blvcksys.util.ChatButtons;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.minecraft.util.Tuple;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  List all global crates.
*/
@AutoConstruct
public class CratesCommand extends APlayerCommand {

  private final ICrateHandler crateHandler;
  private final ITeleportationHandler tp;
  private final ChatUtil chat;

  public CratesCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ICrateHandler crateHandler,
    @AutoInject ChatUtil chat,
    @AutoInject ITeleportationHandler tp
  ) {
    super(
      plugin, logger, cfg, refl,
      "crates",
      "List all crates",
      PlayerPermission.COMMAND_CRATE.toString()
    );

    this.crateHandler = crateHandler;
    this.chat = chat;
    this.tp = tp;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    List<Tuple<CrateModel, List<CrateItemModel>>> crates = crateHandler.listCrates();

    // Begin the head of the component chain with the list prefix
    TextComponent res = new TextComponent(
      cfg.get(ConfigKey.COMMAND_CRATES_LIST_PREFIX)
        .withPrefix()
        .asScalar()
    );

    for (int i = 0; i < crates.size(); i++) {
      CrateModel crate = crates.get(i).a();
      List<CrateItemModel> items = crates.get(i).b();

      // Make the displayed text teleport the player on click
      ChatButtons btn = ChatButtons.buildSimple(
        cfg.get(ConfigKey.COMMAND_CRATES_LIST_FORMAT)
          .withVariable("name", crate.getName())
          .withVariable("sep", i == crates.size() - 1 ? "" : ", "),
        plugin, cfg, () -> {

          // No location set yet
          if (crate.getLoc() == null) {
            p.sendMessage(
              cfg.get(ConfigKey.COMMAND_CRATES_LIST_NO_LOC)
                .withPrefix()
                .withVariable("name", crate.getName())
                .asScalar()
            );
            return;
          }

          tp.requestTeleportation(p, crate.getLoc(), () -> {
            p.sendMessage(
              cfg.get(ConfigKey.COMMAND_CRATES_LIST_TELEPORTED)
                .withPrefix()
                .withVariable("name", crate.getName())
                .asScalar()
            );
          }, null);
        }
      );

      TextComponent crateComp = btn.buildComponent();
      chat.registerButtons(p, btn);

      // Build a unique set of all creators ever involved
      Set<String> creators = new HashSet<>();
      creators.add(crate.getCreator().getName());
      for (CrateItemModel item : items)
        creators.add(item.getCreator().getName());

      // Join all creators into a comma separated list
      String creatorsStr = creators.stream()
        .map(creator ->
          cfg.get(ConfigKey.COMMAND_CRATES_LIST_HOVER_CREATORS_FORMAT)
            .withVariable("creator", creator)
            .asScalar()
        )
        .collect(Collectors.joining(", "));

      // Text when hovering
      Location l = crate.getLoc();
      crateComp.setHoverEvent(new HoverEvent(
        HoverEvent.Action.SHOW_TEXT,
        new Text(
          cfg.get(ConfigKey.COMMAND_CRATES_LIST_HOVER_TEXT)
            .withVariable("created_at", crate.getCreatedAtStr())
            .withVariable("creators", creatorsStr)
            .withVariable("num_items", items.size())
            .withVariable("distance", l == null ? "/" : (int) l.distance(p.getLocation()))
            .withVariable("location", l == null ? "/" : "(" + l.getBlockX() + " | " + l.getBlockY() + " | " + l.getBlockZ() + ")")
            .asScalar()
        )
      ));

      res.addExtra(crateComp);
    }

    // No crates near the player
    if (crates.size() == 0) {
      res.addExtra(new TextComponent(
        cfg.get(ConfigKey.COMMAND_CRATES_LIST_NONE)
          .asScalar()
      ));
    }

    p.spigot().sendMessage(res);
  }
}
