package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.TriResult;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
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
      cfg.get(ConfigKey.GUI_YESNO_TITLE)
        .withVariable("type", i.getArg().type())
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

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);

    // Render the back button, if a callback has been set
    if (inst.getArg().backButton() != null) {
      inst.addBack(18, e -> {
        madeSelection.add(p);
        inst.getArg().backButton().accept(inst);
      });
    }

    // Yes button
    inst.fixedItem(11, () -> (
      new ItemStackBuilder(Material.GREEN_TERRACOTTA)
        .withName(cfg.get(ConfigKey.GUI_YESNO_YES_NAME))
        .withLore(inst.getArg().yesLore())
        .build()
    ), e -> {
      madeSelection.add(p);
      inst.getArg().choice().accept(TriResult.SUCC, inst);
    }, null);

    // No button
    inst.fixedItem(15, () -> (
      new ItemStackBuilder(Material.RED_TERRACOTTA)
        .withName(cfg.get(ConfigKey.GUI_YESNO_NO_NAME))
        .withLore(inst.getArg().noLore())
        .build()
    ), e -> {
      madeSelection.add(p);
      inst.getArg().choice().accept(TriResult.ERR, inst);
    }, null);

    return true;
  }
}