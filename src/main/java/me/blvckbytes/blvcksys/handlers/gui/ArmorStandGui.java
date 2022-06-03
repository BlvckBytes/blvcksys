package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IArmorStandHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.MoveablePart;
import me.blvckbytes.blvcksys.packets.communicators.armorstand.ArmorStandProperties;
import me.blvckbytes.blvcksys.persistence.models.ArmorStandModel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

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

  // Scaling factor used for movement delta before applying it to the angle
  private static final float MOVEMENT_SCALING = .5F;

  @AllArgsConstructor
  private static class MoveRequest {
    MoveablePart part;                    // Part which is being moved
    ArmorStandModel model;                // Target armor stand
    ArmorStandProperties props;           // Properties under modification
    boolean enabled;                      // Whether movement is enabled
    @Nullable BukkitTask enableTimeout;   // Movement enable timeout task
    @Nullable Location prevLoc;           // Previous movement location
  }

  // Players mapped to their current mouse button state
  private final Map<Player, MoveRequest> moving;
  private final IArmorStandHandler standHandler;

  public ArmorStandGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IArmorStandHandler standHandler
  ) {
    super(5, "", i -> (
      cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_NAME).
        withVariable("name", i.getArg().getName())
    ), plugin, cfg, textures);

    this.moving = new HashMap<>();
    this.standHandler = standHandler;
  }

  @Override
  protected boolean closed(GuiInstance<ArmorStandModel> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<ArmorStandModel> inst) {
    Player p = inst.getViewer();
    ArmorStandModel model = inst.getArg();
    ArmorStandProperties props = standHandler.getProperties(model.getName()).orElse(null);

    if (props == null) {
      p.sendMessage(
        cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_NO_PROPS)
          .withPrefix()
          .withVariable("name", model.getName())
          .asScalar()
      );
      return false;
    }

    // Just to test out head rotation
    props.setHelmet(new ItemStack(Material.SKELETON_SKULL));
    standHandler.setProperties(model.getName(), props, false);

    // TODO: Remove again when the user's done
    moving.put(p, new MoveRequest(MoveablePart.HEAD, model, props, false, null, null));
    return true;
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onMove(PlayerMoveEvent e) {
    Player p = e.getPlayer();

    // Not in a customization session or not pressing the mouse button
    MoveRequest req = moving.get(p);
    if (req == null || !req.enabled || req.prevLoc == null)
      return;

    // Calculate movement delta and update the previous location
    Location nextLoc = p.getLocation().clone();
    double dYaw = normalizeAngle(nextLoc.getYaw()) - normalizeAngle(req.prevLoc.getYaw());
    double dPitch = normalizeAngle(nextLoc.getPitch() * 2) - normalizeAngle(req.prevLoc.getPitch() * 2);
    req.prevLoc = nextLoc;

    // Scale movement
    dYaw *= MOVEMENT_SCALING;
    dPitch *= MOVEMENT_SCALING;

    // Add the movement delta to the euler angle
    req.part.set(req.props, req.part.get(req.props).add(Math.toRadians(dPitch), Math.toRadians(dYaw), 0));

    // Set the properties without persisting yet
    standHandler.setProperties(req.model.getName(), req.props, false);
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    Player p = e.getPlayer();

    if (!(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR))
      return;

    // Not in a customization session
    MoveRequest req = moving.get(p);
    if (req == null)
      return;

    e.setCancelled(true);

    // Cancel previous timeouts
    if (req.enableTimeout != null)
      req.enableTimeout.cancel();

    // Set the previous location on enabled delta
    if (!req.enabled)
      req.prevLoc = p.getLocation().clone();

    // Create a timeout for disabling move when the button is released again
    req.enabled = true;
    req.enableTimeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
      req.enabled = false;
      req.enableTimeout = null;
    }, 5L);
  }

  /**
   * Normalize any angle in degrees to be within the range [0;360]
   * @param angle Input angle
   * @return Normalized output angle
   */
  private float normalizeAngle(float angle) {
    while (angle < 0)
      angle += 360;

    while (angle > 360)
      angle -= 360;

    return angle;
  }
}
