package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.packets.modifiers.WindowOpenSniffer;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;

@AutoConstruct
public class VirtualFurnaceGui extends AGui<VirtualFurnace> {

  private final WindowOpenSniffer windowSniffer;

  public VirtualFurnaceGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject WindowOpenSniffer windowSniffer
  ) {
    super(1, "", i -> (
      cfg.get(ConfigKey.GUI_VFURNACE_TITLE)
        .withVariable("name", "#" + i.getArg().getIndex())
    ), plugin, cfg, InventoryType.FURNACE);

    this.windowSniffer = windowSniffer;
  }

  @Override
  protected boolean closed(GuiInstance<VirtualFurnace> inst) {
    // Stop rendering state
    inst.getArg().setContainerId(null);
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<VirtualFurnace> inst) {
    VirtualFurnace vf = inst.getArg();

    // Item to be smelted
    inst.fixedItem("0", vf::getSmelting, e -> {
      e.setCancelled(false);
      Bukkit.getScheduler().runTask(plugin, () -> vf.setSmelting(inst.getInv().getItem(0)));
    }, 1);

    // Power source
    inst.fixedItem("1", vf::getPowerSource, e -> {
      e.setCancelled(false);
      Bukkit.getScheduler().runTask(plugin, () -> vf.setPowerSource(inst.getInv().getItem(1)));
    }, 1);

    // Smelted output
    inst.fixedItem("2", vf::getSmelted, e -> {
      e.setCancelled(false);
      Bukkit.getScheduler().runTask(plugin, () -> vf.setSmelted(inst.getInv().getItem(2)));
    }, 1);

    return true;
  }

  @Override
  protected void opened(GuiInstance<VirtualFurnace> inst) {
    Bukkit.getScheduler().runTask(plugin, () -> {
      // Start rendering state
      inst.getArg().setContainerId(windowSniffer.getTopInventoryWindowId(inst.getViewer()));
    });
  }
}
