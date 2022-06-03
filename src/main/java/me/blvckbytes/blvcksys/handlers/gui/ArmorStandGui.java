package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.ArmorStandModel;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/03/2022

  Presents all possible settings for a fake armor stand and allows
  for angle customization using move events while the right mouse button
  is being pressed down.
*/
@AutoConstruct
public class ArmorStandGui extends AGui<ArmorStandModel> {

  // Players mapped to their current mouse button state
  private final Map<Player, Tuple<Boolean, BukkitTask>> moving;

  public ArmorStandGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures
  ) {
    super(5, "", i -> (
      cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_NAME).
        withVariable("name", i.getArg().getName())
    ), plugin, cfg, textures);

    this.moving = new HashMap<>();
  }

  @Override
  protected boolean closed(GuiInstance<ArmorStandModel> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<ArmorStandModel> inst) {
    // TODO: Remove again when the user's done
    moving.put(inst.getViewer(), new Tuple<>(false, null));
    return true;
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onMove(PlayerMoveEvent e) {
    Player p = e.getPlayer();

    // Not in a customization session or not pressing the mouse button
    Tuple<Boolean, BukkitTask> enabled = moving.get(p);
    if (enabled == null || !enabled.a())
      return;

    // TODO: Manipulate armor stand rotations by the delta of this move
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    Player p = e.getPlayer();

    if (!(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR))
      return;

    // Not in a customization session
    Tuple<Boolean, BukkitTask> enabled = moving.get(p);
    if (enabled == null)
      return;

    // Cancel previous timeouts
    if (enabled.b() != null)
      enabled.b().cancel();

    // Create a timeout for disabling move when the button is released again
    BukkitTask timeoutHandle = Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (moving.containsKey(p))
        moving.put(p, new Tuple<>(false, null));
    }, 5L);

    moving.put(p, new Tuple<>(true, timeoutHandle));
  }
}
