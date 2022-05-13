package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.ImageFrameHandler;
import me.blvckbytes.blvcksys.handlers.ItemFrameGroup;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.ImageFrameModel;
import me.blvckbytes.blvcksys.persistence.models.ImageFrameType;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Create or delete image item frame groups by their name.
*/
@AutoConstruct
public class ImageFrameCommand extends APlayerCommand {

  private final ImageFrameHandler iframe;
  private final IPersistence pers;

  private enum FrameAction {
    CREATE,
    DELETE,
    RELOAD
  }

  public ImageFrameCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ImageFrameHandler iframe,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "imageframe,iframe",
      "Manage image item frames",
      PlayerPermission.COMMAND_IFRAME,
      new CommandArgument("<name>", "Name of the target group"),
      new CommandArgument("<action>", "Action to perform"),
      new CommandArgument("[type]", "Type of the new group"),
      new CommandArgument("[resource]", "Resource the group will render")
    );

    this.iframe = iframe;
    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest existing frames
    if (currArg == 0)
      return suggestModels(args, currArg, ImageFrameModel.class, "name", pers);

    // Suggest actions
    if (currArg == 1)
      return suggestEnum(args, currArg, FrameAction.class);

    // Suggest types
    if (currArg == 2)
      return suggestEnum(args, currArg, ImageFrameType.class);

    // Suggest resource placeholder
    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    FrameAction action = parseEnum(FrameAction.class, args, 1, null);

    if (action == FrameAction.DELETE) {
      if (!iframe.deleteGroup(name)) {
        p.sendMessage(
          cfg.get(ConfigKey.IMAGEFRAME_GROUP_NOT_FOUND)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.IMAGEFRAME_GROUP_DELETED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == FrameAction.RELOAD) {
      if (!iframe.reloadGroup(name)) {
        p.sendMessage(
          cfg.get(ConfigKey.IMAGEFRAME_GROUP_NOT_FOUND)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.IMAGEFRAME_GROUP_RELOADED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    // Has to point to a block
    Block b = p.getTargetBlockExact(10, FluidCollisionMode.NEVER);
    if (b == null) {
      p.sendMessage(
        cfg.get(ConfigKey.IMAGEFRAME_NO_BLOCK)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Has to point to a frame
    ItemFrame frame = iframe.findNearbyFrame(b.getLocation());
    if (frame == null) {
      p.sendMessage(
        cfg.get(ConfigKey.IMAGEFRAME_NO_FRAME)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    if (action == FrameAction.CREATE) {

      // Check for an existing group for this frame
      ItemFrameGroup group = iframe.findGroupByMember(frame);
      if (group != null) {
        p.sendMessage(
          cfg.get(ConfigKey.IMAGEFRAME_ALREADY_REGISTERED)
            .withPrefix()
            .withVariable("name", group.getName())
            .asScalar()
        );
        return;
      }

      ImageFrameType type = parseEnum(ImageFrameType.class, args, 2, null);
      String resource = argvar(args, 3, "");

      if (!iframe.createGroup(p, name, frame.getLocation(), type, resource)) {
        p.sendMessage(
          cfg.get(ConfigKey.IMAGEFRAME_GROUP_EXISTS)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.IMAGEFRAME_GROUP_CREATED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }
  }
}
