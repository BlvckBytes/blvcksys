package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IImageFrameHandler;
import me.blvckbytes.blvcksys.handlers.ITeleportationHandler;
import me.blvckbytes.blvcksys.handlers.ImageFrameHandler;
import me.blvckbytes.blvcksys.handlers.ItemFrameGroup;
import me.blvckbytes.blvcksys.persistence.models.ImageFrameModel;
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

import java.util.List;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/12/2022

  List all image-frames near you.
*/
@AutoConstruct
public class ImageFramesCommand extends APlayerCommand {

  // What radius to use as a default when no arg has been specified
  private static final float RADIUS_FALLBACK = 50;

  private final IImageFrameHandler iframe;
  private final ChatUtil chat;
  private final ITeleportationHandler tp;

  public ImageFramesCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ImageFrameHandler iframe,
    @AutoInject ChatUtil chat,
    @AutoInject ITeleportationHandler tp
  ) {
    super(
      plugin, logger, cfg, refl,
      "imageframes,iframes",
      "List nearby image frames",
      PlayerPermission.COMMAND_IFRAME,
      new CommandArgument("[radius]", "Radius to list within")
    );

    this.iframe = iframe;
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
    List<Tuple<ImageFrameModel, ItemFrameGroup>> groups = iframe.findNearbyGroups(p.getLocation(), radius);

    // Begin the head of the component chain with the list prefix
    TextComponent res = new TextComponent(
      cfg.get(ConfigKey.COMMAND_IMAGEFRAMES_LIST_PREFIX)
        .withPrefix()
        .withVariable("radius", radius)
        .asScalar()
    );

    // Add all groups to the list
    for (int i = 0; i < groups.size(); i++) {
      ImageFrameModel model = groups.get(i).a();
      ItemFrameGroup group = groups.get(i).b();

      Location l = model.getLoc();
      String r = model.getResource();

      // Make the displayed text teleport the player on click
      ChatButtons btn = ChatButtons.buildSimple(
        cfg.get(ConfigKey.COMMAND_IMAGEFRAMES_LIST_FORMAT)
          .withVariable("name", group.getName())
          .withVariable("sep", i == groups.size() - 1 ? "" : ", "),
        plugin, cfg, () -> {
          tp.requestTeleportation(p, l, () -> {
            p.sendMessage(
              cfg.get(ConfigKey.COMMAND_IMAGEFRAMES_LIST_TELEPORTED)
                .withPrefix()
                .withVariable("name", group.getName())
                .asScalar()
            );
          }, null);
        }
      );

      TextComponent groupComp = btn.buildComponent();
      chat.registerButtons(p, btn);

      // Text when hovering
      groupComp.setHoverEvent(new HoverEvent(
        HoverEvent.Action.SHOW_TEXT,
        new Text(
          cfg.get(ConfigKey.COMMAND_IMAGEFRAMES_LIST_HOVER_TEXT)
            .withVariable("created_at", model.getCreatedAtStr())
            .withVariable("updated_at", model.getUpdatedAtStr())
            .withVariable("creator", model.getCreator().getName())
            .withVariable("num_members", group.getNumMembers())
            .withVariable("dimensions", group.getWidth() + "x" + group.getHeight())
            .withVariable("type", model.getType().name())
            .withVariable("resource", (r == null || r.isBlank()) ? "/" : r)
            .withVariable("location", "(" + l.getBlockX() + " | " + l.getBlockY() + " | " + l.getBlockZ() + ")")
            .withVariable("distance", (int) l.distance(p.getLocation()))
            .asScalar()
        )
      ));

      res.addExtra(groupComp);
    }

    // No image-frames near the player
    if (groups.size() == 0) {
      res.addExtra(new TextComponent(
        cfg.get(ConfigKey.COMMAND_IMAGEFRAMES_LIST_NONE)
          .asScalar()
      ));
    }

    p.spigot().sendMessage(res);
  }
}
