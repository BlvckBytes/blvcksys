package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.TriResult;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Offers the viewer a chance to either confirm or cancel an action.
  Argument: function(confirmed, guiInventory) returning whether to close the inventory, where
  confirmed has three states: SUCC=confirmed, ERR=cancelled, EMPTY=inv closed
*/
@AutoConstruct
public class ConfirmationGui extends AGui<BiFunction<TriResult, Inventory, Boolean>> {

  // Players which made a selection in the GUI don't trigger the callback on close
  private final Set<Player> madeSelection;

  public ConfirmationGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures
  ) {
    super(3, "", i -> cfg.get(ConfigKey.GUI_CONFIRMATION_TITLE), plugin, cfg, textures);
    this.madeSelection = new HashSet<>();
  }

  @Override
  protected boolean closed(GuiInstance<BiFunction<TriResult, Inventory, Boolean>> inst) {
    if (!madeSelection.remove(inst.getViewer()))
      inst.getArg().apply(TriResult.EMPTY, inst.getInv());
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<BiFunction<TriResult, Inventory, Boolean>> inst) {
    Player p = inst.getViewer();

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);

    inst.fixedItem(11, () -> (
      new ItemStackBuilder(Material.GREEN_TERRACOTTA)
        .withName(cfg.get(ConfigKey.GUI_CONFIRMATION_CONFIRM_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CONFIRMATION_CONFIRM_LORE))
        .build()
    ), e -> {
      madeSelection.add(p);
      if (inst.getArg().apply(TriResult.SUCC, inst.getInv()))
        inst.close();
    }, null);

    inst.fixedItem(15, () -> (
      new ItemStackBuilder(Material.RED_TERRACOTTA)
        .withName(cfg.get(ConfigKey.GUI_CONFIRMATION_CANCEL_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CONFIRMATION_CANCEL_LORE))
        .build()
    ), e -> {
      madeSelection.add(p);
      if (inst.getArg().apply(TriResult.ERR, inst.getInv()))
        inst.close();
    }, null);

    return true;
  }
}
