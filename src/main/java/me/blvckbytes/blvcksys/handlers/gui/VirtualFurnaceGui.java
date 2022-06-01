package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.packets.modifiers.WindowOpenSniffer;
import me.blvckbytes.blvcksys.util.MCReflect;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;

@AutoConstruct
public class VirtualFurnaceGui extends AGui<Object> {

  private final MCReflect refl;
  private final WindowOpenSniffer windowSniffer;

  public VirtualFurnaceGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject MCReflect refl,
    @AutoInject WindowOpenSniffer windowSniffer
  ) {
    super(1, "", i -> (
      cfg.get(ConfigKey.GUI_VFURNACE_TITLE)
        .withVariable("name", "#1")
    ), plugin, cfg, textures, InventoryType.FURNACE);

    this.refl = refl;
    this.windowSniffer = windowSniffer;
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();
    VirtualFurnace vf = new VirtualFurnace(p);

    inst.setTickReceiver(time -> {
      // Keep the furnace inventory at the client up to date
      vf.tick(windowSniffer.getTopInventoryWindowId(p), refl);
      inst.redraw("*");
    });

    // Item to be smelted
    inst.fixedItem(0, vf::getSmelting, e -> {
      e.setCancelled(false);
      Bukkit.getScheduler().runTask(plugin, () -> vf.setSmelting(inst.getInv().getItem(0)));
    });

    // Power source
    inst.fixedItem(1, vf::getPowerSource, e -> {
      e.setCancelled(false);
      Bukkit.getScheduler().runTask(plugin, () -> vf.setPowerSource(inst.getInv().getItem(1)));
    });

    // Smelted output
    inst.fixedItem(2, vf::getSmelted, e -> {
      e.setCancelled(false);
      Bukkit.getScheduler().runTask(plugin, () -> vf.setSmelted(inst.getInv().getItem(2)));
    });

    return true;
  }
}
