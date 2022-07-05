package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.sections.GuiLayoutSection;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.TriResult;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/30/2022

  Offers the viewer a chance to either choose yes or no.
*/
@AutoConstruct
public class YesNoGui extends AGui<YesNoParam> {

  // Players which made a selection in the GUI don't trigger the callback on close
  private final Set<Player> madeSelection;

  public YesNoGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures
  ) {
    super(3, "", i -> (
      ConfigValue.immediate(i.getArg().type())
    ), plugin, cfg, textures);

    this.madeSelection = new HashSet<>();
  }

  @Override
  protected boolean closed(GuiInstance<YesNoParam> inst) {
    if (!madeSelection.remove(inst.getViewer()))
      inst.getArg().choice().accept(TriResult.EMPTY, inst);
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<YesNoParam> inst) {
    Player p = inst.getViewer();
    IStdGuiParamProvider paramProvider = inst.getArg().paramProvider();
    GuiLayoutSection layout = inst.getArg().layout();

    if (!inst.applyLayoutParameters(layout, paramProvider))
      inst.addFill(paramProvider);

    Map<String, String> slots = layout != null ? layout.getSlots() : new HashMap<>();

    // Render the back button, if a callback has been set
    if (inst.getArg().backButton() != null) {
      inst.addBack(
        slots.getOrDefault("back", "18"), paramProvider,
        e -> {
          madeSelection.add(p);
          inst.getArg().backButton().accept(inst);
        }
      );
    }

    // Yes button
    inst.fixedItem(
      slots.getOrDefault("yes", "11"),
      () -> inst.getArg().yesButton(), e -> {
        madeSelection.add(p);
        inst.getArg().choice().accept(TriResult.SUCC, inst);
      },
      null
    );

    // No button
    inst.fixedItem(
      slots.getOrDefault("no", "15"),
      () -> inst.getArg().noButton(),
      e -> {
        madeSelection.add(p);
        inst.getArg().choice().accept(TriResult.ERR, inst);
      },
      null
    );

    return true;
  }
}
