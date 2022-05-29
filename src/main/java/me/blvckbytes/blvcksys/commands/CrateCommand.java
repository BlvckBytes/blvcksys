package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.adapters.IRegionAdapter;
import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.ICrateHandler;
import me.blvckbytes.blvcksys.handlers.gui.AnimationType;
import me.blvckbytes.blvcksys.handlers.gui.CrateContentGui;
import me.blvckbytes.blvcksys.handlers.gui.CrateDrawLayout;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import me.blvckbytes.blvcksys.persistence.models.ParticleEffectColor;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Manage crates by creating or changing their item contents.
*/
@AutoConstruct
public class CrateCommand extends APlayerCommand {

  private enum CrateAction {
    CREATE,
    DELETE,
    ADDITEM,
    LISTITEMS,
    SETCHEST,
    SETLAYOUT,
    SETCOLOR
  }

  private final CrateContentGui crateContentGui;
  private final ICrateHandler crateHandler;
  private final IPersistence pers;
  private final IRegionAdapter regions;

  public CrateCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ICrateHandler crateHandler,
    @AutoInject IPersistence pers,
    @AutoInject IRegionAdapter regions,
    @AutoInject CrateContentGui crateContentGui
  ) {
    super(
      plugin, logger, cfg, refl,
      "crate",
      "Manage crates and their items",
      PlayerPermission.COMMAND_CRATE,
      new CommandArgument("<name>", "Name of the crate"),
      new CommandArgument("<action>", "Action to perform"),
      new CommandArgument("[value]", "Value to set")
    );

    this.crateHandler = crateHandler;
    this.pers = pers;
    this.regions = regions;
    this.crateContentGui = crateContentGui;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestModels(args, currArg, CrateModel.class, "name", pers);

    if (currArg == 1)
      return suggestEnum(args, currArg, CrateAction.class);

    if (currArg == 2) {
      if (args[1].equalsIgnoreCase(CrateAction.SETLAYOUT.name()))
        return suggestEnum(args, currArg, CrateDrawLayout.class);
      if (args[1].equalsIgnoreCase(CrateAction.SETCOLOR.name()))
        return suggestEnum(args, currArg, ParticleEffectColor.class);
      return Stream.of(getArgumentPlaceholder(currArg));
    }

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    CrateAction action = parseEnum(CrateAction.class, args, 1, null);

    if (action == CrateAction.CREATE) {
      if (crateHandler.createCrate(p, name)) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_CREATED)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_CRATE_EXISTS)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == CrateAction.DELETE) {
      if (crateHandler.deleteCrate(name)) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_DELETED)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_CRATE_NOT_EXISTING)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == CrateAction.SETLAYOUT) {
      CrateDrawLayout layout = parseEnum(CrateDrawLayout.class, args, 2, null);
      if (crateHandler.setCrateLayout(name, layout)) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_LAYOUT_SET)
            .withPrefix()
            .withVariable("name", name)
            .withVariable("layout", layout.name())
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_CRATE_NOT_EXISTING)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == CrateAction.SETCOLOR) {
      ParticleEffectColor color = parseEnum(ParticleEffectColor.class, args, 2, null);
      if (crateHandler.setCrateParticleEffectColor(name, color)) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_COLOR_SET)
            .withPrefix()
            .withVariable("name", name)
            .withVariable("color", color.name())
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_CRATE_NOT_EXISTING)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == CrateAction.SETCHEST) {

      // Not a chest block
      Block b = p.getTargetBlockExact(10, FluidCollisionMode.NEVER);
      if (b == null || !(b.getState() instanceof Chest)) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_NOCHEST)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      // Cannot build here
      Location loc = b.getLocation();
      if (!regions.canBuild(p, loc)) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_NOBUILD)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      if (crateHandler.moveCrate(name, b.getLocation())) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_MOVED)
            .withPrefix()
            .withVariable("name", name)
            .withVariable("location", "(" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ() + ")")
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_CRATE_NOT_EXISTING)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == CrateAction.LISTITEMS) {
      CrateModel crate = crateHandler.getCrate(name).orElse(null);

      if (crate == null) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_NOT_EXISTING)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      crateContentGui.show(p, new Tuple<>(crate, true), AnimationType.SLIDE_DOWN);
      return;
    }

    if (action == CrateAction.ADDITEM) {

      // Has to have something in their hand
      ItemStack stack = p.getInventory().getItemInMainHand();
      if (stack.getType() == Material.AIR) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_NOITEM)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      float probability = parseFloat(args, 2);
      if (probability <= 0 || probability >= 100) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_ITEM_INVALID_PROBABILITY)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      if (crateHandler.addItem(p, name, stack, probability)) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_ITEM_ADDED)
            .withPrefix()
            .withVariable("name", name)
            .withVariable("probability", probability)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_CRATE_NOT_EXISTING)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }
  }
}
