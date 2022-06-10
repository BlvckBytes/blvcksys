package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.IVirtualFurnaceHandler;
import me.blvckbytes.blvcksys.util.SymbolicHead;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Manage all virtual furnaces in a GUI with rich information on display.
*/
@AutoConstruct
public class FurnacesGui extends AGui<Object> {

  private final IVirtualFurnaceHandler furnaceHandler;
  private final VirtualFurnaceGui furnaceGui;

  public FurnacesGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IVirtualFurnaceHandler furnaceHandler,
    @AutoInject VirtualFurnaceGui furnaceGui
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_FURNACES)
        .withVariable("name", i.getViewer().getName())
    ), plugin, cfg, textures);

    this.furnaceHandler = furnaceHandler;
    this.furnaceGui = furnaceGui;
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);
    inst.addPagination(37, 40, 43);

    for (VirtualFurnace furnace : furnaceHandler.listFurnaces(p)) {
      inst.addPagedItem(
        i -> buildFurnaceIcon(furnace),
        fe -> inst.switchTo(null, furnaceGui, furnace),
        1
      );
    }

    inst.fixedItem(26, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.GREEN_PLUS.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_FURNACES_CREATE_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_FURNACES_CREATE_LORE)
            .withVariable("used", furnaceHandler.getUsedNumberOfFurnaces(p))
            .withVariable("available", furnaceHandler.getAvailableNumberOfFurnaces(p))
        )
        .build()
    ), e -> {
      int available = furnaceHandler.getAvailableNumberOfFurnaces(p);
      int used = furnaceHandler.getUsedNumberOfFurnaces(p);
      if (available <= used) {
        p.sendMessage(
          cfg.get(ConfigKey.GUI_FURNACES_MAX_REACHED)
            .withPrefix()
            .withVariable("available", available)
            .asScalar()
        );
        return;
      }

      VirtualFurnace furnace = furnaceHandler.accessFurnace(p, used + 1);

      // "Touch" the next furnace in the sequence
      inst.addPagedItem(
        s -> buildFurnaceIcon(furnace),
        fe -> inst.switchTo(null, furnaceGui, furnace),
        1
      );

      // Go to the last page to make sure the new furnace is visible
      inst.lastPage(AnimationType.SLIDE_LEFT);

      // Redraw the new button to update the slot indicator
      inst.redraw("26");
    });

    return true;
  }

  /**
   * Build an icon which represents the given furnace
   * @param vf Target furnace
   * @return Icon to display
   */
  private ItemStack buildFurnaceIcon(VirtualFurnace vf) {
    FurnaceState state = vf.getState();

    Material mat = Material.FURNACE;
    ConfigKey stateKey = ConfigKey.GUI_FURNACES_FURNACE_STATE_EMPTY;

    switch (state) {
      case SMELTING -> {
        mat = Material.ORANGE_STAINED_GLASS;
        stateKey = ConfigKey.GUI_FURNACES_FURNACE_STATE_SMELTING;
      }
      case OUT_OF_FUEL -> {
        mat = Material.RED_STAINED_GLASS;
        stateKey = ConfigKey.GUI_FURNACES_FURNACE_STATE_OUT_OF_FUEL;
      }
      case FULL -> {
        mat = Material.PURPLE_STAINED_GLASS;
        stateKey = ConfigKey.GUI_FURNACES_FURNACE_STATE_FULL;
      }
      case HAS_REMAINS -> {
        mat = Material.YELLOW_STAINED_GLASS;
        stateKey = ConfigKey.GUI_FURNACES_FURNACE_STATE_HAS_REMAINS;
      }
    }

    return new ItemStackBuilder(mat)
      .withName(
        cfg.get(ConfigKey.GUI_FURNACES_FURNACE_NAME)
          .withVariable("index", vf.getIndex())
      )
      .withLore(
        cfg.get(ConfigKey.GUI_FURNACES_FURNACE_LORE)
          .withVariable("type_smelting", vf.getSmelting() == null ? "/" : formatConstant(vf.getSmelting().getType().name()))
          .withVariable("finished_items", vf.getSmelted() == null ? 0 : vf.getSmelted().getAmount())
          .withVariable("pending_items", vf.getSmelting() == null ? 0 : vf.getSmelting().getAmount())
          .withVariable("number_fuel", vf.getPowerSource() == null ? 0 : vf.getPowerSource().getAmount())
          .withVariable("type_fuel", vf.getPowerSource() == null ? "/" : formatConstant(vf.getPowerSource().getType().name()))
          .withVariable("state", cfg.get(stateKey).asScalar())
      )
      .build();
  }
}