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
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.SymbolicHead;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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

  /*
    What I want to do:

    Rotate body-parts while holding the right mouse button until I type something in the chat, for:
    - head
    - body
    - leg l, leg r
    - arm l, arm r

    Change helmet
    Change chestplate
    Change leggings
    Change boots
    Change main hand
    Change off hand (is this possible?)

    And for all items, either set an item from the inventory
    or use the item editor to edit an existing item
   */

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
  private final ChatUtil chatUtil;

  public ArmorStandGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IArmorStandHandler standHandler,
    @AutoInject ChatUtil chatUtil
  ) {
    super(6, "", i -> (
      cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_NAME).
        withVariable("name", i.getArg().getName())
    ), plugin, cfg, textures);

    this.moving = new HashMap<>();
    this.standHandler = standHandler;
    this.chatUtil = chatUtil;
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

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);

    /////////////////////////////////// Body Columns ////////////////////////////////////

    inst.fixedItem(10, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.LETTER_H.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_HELMET_NAME))
        .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_HELMET_LORE))
        .build()
    ), null);

    inst.fixedItem(11, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.LETTER_C.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_CHESTPLATE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_CHESTPLATE_LORE))
        .build()
    ), null);

    inst.fixedItem(12, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.LETTER_L.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_LEGGINGS_NAME))
        .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_LEGGINGS_LORE))
        .build()
    ), null);

    inst.fixedItem(13, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.LETTER_B.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_BOOTS_NAME))
        .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_BOOTS_LORE))
        .build()
    ), null);

    inst.fixedItem(14, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.LETTER_L.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_LEFT_ARM_NAME))
        .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_LEFT_ARM_LORE))
        .build()
    ), null);

    inst.fixedItem(15, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.LETTER_R.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_RIGHT_ARM_NAME))
        .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_RIGHT_ARM_LORE))
        .build()
    ), null);

    /////////////////////////////////// Equipment Slots ////////////////////////////////////

    inst.fixedItem(19, () -> (
      props.getHelmet() == null ?
        new ItemStackBuilder(Material.BARRIER)
          .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_NAME))
          .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_LORE))
          .build()
        : props.getHelmet()
    ), e -> {

    });

    inst.fixedItem(20, () -> (
      props.getChestplate() == null ?
        new ItemStackBuilder(Material.BARRIER)
          .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_NAME))
          .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_LORE))
          .build()
        : props.getChestplate()
    ), e -> {

    });

    inst.fixedItem(21, () -> (
      props.getLeggings() == null ?
        new ItemStackBuilder(Material.BARRIER)
          .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_NAME))
          .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_LORE))
          .build()
        : props.getLeggings()
    ), e -> {

    });

    inst.fixedItem(22, () -> (
      props.getBoots() == null ?
        new ItemStackBuilder(Material.BARRIER)
          .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_NAME))
          .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_LORE))
          .build()
        : props.getBoots()
    ), e -> {

    });

    inst.fixedItem(23, () -> (
      props.getOffHand() == null ?
        new ItemStackBuilder(Material.BARRIER)
          .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_NAME))
          .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_LORE))
          .build()
        : props.getOffHand()
    ), e -> {

    });

    inst.fixedItem(24, () -> (
      props.getHand() == null ?
        new ItemStackBuilder(Material.BARRIER)
          .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_NAME))
          .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_LORE))
          .build()
        : props.getHand()
    ), e -> {

    });

    /////////////////////////////////// Body Poses ////////////////////////////////////

    inst.fixedItem(28, () -> (
      new ItemStackBuilder(Material.GOLDEN_AXE)
        .hideAttributes()
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_LORE)
            .withVariable("bodypart", formatConstant(MoveablePart.HEAD.name()))
        )
        .build()
    ), e -> initializePoseCustomize(inst, MoveablePart.HEAD));

    inst.fixedItem(29, () -> (
      new ItemStackBuilder(Material.GOLDEN_AXE)
        .hideAttributes()
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_LORE)
            .withVariable("bodypart", formatConstant(MoveablePart.BODY.name()))
        )
        .build()
    ), e -> initializePoseCustomize(inst, MoveablePart.BODY));

    inst.fixedItem(30, () -> (
      new ItemStackBuilder(Material.GOLDEN_AXE)
        .hideAttributes()
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_LORE)
            .withVariable("bodypart", formatConstant(MoveablePart.LEFT_LEG.name()))
        )
        .build()
    ), e -> initializePoseCustomize(inst, MoveablePart.LEFT_LEG));

    inst.fixedItem(31, () -> (
      new ItemStackBuilder(Material.GOLDEN_AXE)
        .hideAttributes()
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_LORE)
            .withVariable("bodypart", formatConstant(MoveablePart.RIGHT_LEG.name()))
        )
        .build()
    ), e -> initializePoseCustomize(inst, MoveablePart.RIGHT_LEG));

    inst.fixedItem(32, () -> (
      new ItemStackBuilder(Material.GOLDEN_AXE)
        .hideAttributes()
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_LORE)
            .withVariable("bodypart", formatConstant(MoveablePart.LEFT_ARM.name()))
        )
        .build()
    ), e -> initializePoseCustomize(inst, MoveablePart.LEFT_ARM));

    inst.fixedItem(33, () -> (
      new ItemStackBuilder(Material.GOLDEN_AXE)
        .hideAttributes()
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_LORE)
            .withVariable("bodypart", formatConstant(MoveablePart.RIGHT_ARM.name()))
        )
        .build()
    ), e -> initializePoseCustomize(inst, MoveablePart.RIGHT_ARM));

    /////////////////////////////////// Boolean Flags ////////////////////////////////////

    inst.fixedItem(16, () -> (
      new ItemStackBuilder(props.isVisible() ? Material.GREEN_DYE : Material.GRAY_DYE)
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_VISIBILITY_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_VISIBILITY_LORE)
            .withVariable("state", cfg.get(props.isVisible() ? ConfigKey.GUI_AS_CUSTOMIZE_STATE_YES : ConfigKey.GUI_AS_CUSTOMIZE_STATE_NO))
        )
        .build()
    ), e -> {
      props.setVisible(!props.isVisible());
      standHandler.setProperties(model.getName(), props, true);
      inst.redraw("16");
    });

    inst.fixedItem(25, () -> (
      new ItemStackBuilder(props.isArms() ? Material.GREEN_DYE : Material.GRAY_DYE)
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ARMS_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ARMS_LORE)
            .withVariable("state", cfg.get(props.isArms() ? ConfigKey.GUI_AS_CUSTOMIZE_STATE_YES : ConfigKey.GUI_AS_CUSTOMIZE_STATE_NO))
        )
        .build()
    ), e -> {
      props.setArms(!props.isArms());
      standHandler.setProperties(model.getName(), props, true);
      inst.redraw("25");
    });

    inst.fixedItem(34, () -> (
      new ItemStackBuilder(props.isSmall() ? Material.GREEN_DYE : Material.GRAY_DYE)
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_SMALL_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_SMALL_LORE)
            .withVariable("state", cfg.get(props.isSmall() ? ConfigKey.GUI_AS_CUSTOMIZE_STATE_YES : ConfigKey.GUI_AS_CUSTOMIZE_STATE_NO))
        )
        .build()
    ), e -> {
      props.setSmall(!props.isSmall());
      standHandler.setProperties(model.getName(), props, true);
      inst.redraw("34");
    });

    inst.fixedItem(43, () -> (
      new ItemStackBuilder(props.isBaseplate() ? Material.GREEN_DYE : Material.GRAY_DYE)
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_BASEPLATE_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_BASEPLATE_LORE)
            .withVariable("state", cfg.get(props.isBaseplate() ? ConfigKey.GUI_AS_CUSTOMIZE_STATE_YES : ConfigKey.GUI_AS_CUSTOMIZE_STATE_NO))
        )
        .build()
    ), e -> {
      props.setBaseplate(!props.isBaseplate());
      standHandler.setProperties(model.getName(), props, true);
      inst.redraw("43");
    });

    /////////////////////////////////// Name (+Visibility) ////////////////////////////////////

    inst.fixedItem(37, () -> (
      new ItemStackBuilder(props.isNameVisible() ? Material.GREEN_DYE : Material.GRAY_DYE)
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_NAME_VISIBILITY_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_NAME_VISIBILITY_LORE)
            .withVariable("state", cfg.get(props.isNameVisible() ? ConfigKey.GUI_AS_CUSTOMIZE_STATE_YES : ConfigKey.GUI_AS_CUSTOMIZE_STATE_NO))
        )
        .build()
    ), e -> {
      props.setNameVisible(!props.isNameVisible());
      standHandler.setProperties(model.getName(), props, true);
      inst.redraw("37");
    });


    inst.fixedItem(38, () -> (
      new ItemStackBuilder(Material.NAME_TAG)
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_DISPLAYNAME_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_DISPLAYNAME_LORE)
            .withVariable("name", props.getName() == null ? "/" : props.getName())
        )
        .build()
    ), e -> {
      // Close the instance and spawn a new chat prompt for the name
      inst.close();

      chatUtil.registerPrompt(
        inst.getViewer(),
        cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_DISPLAYNAME_PROMPT)
          .withPrefix()
          .asScalar(),

        // Name entered, change, store and reopen
        name -> {
          props.setName(ChatColor.translateAlternateColorCodes('&', name));
          standHandler.setProperties(model.getName(), props, true);
          inst.redraw("37");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_DISPLAYNAME_CHANGED)
              .withPrefix()
              .asScalar()
          );

          inst.reopen(AnimationType.SLIDE_UP);
        },

        // Cancellation reopens the editor
        () -> inst.reopen(AnimationType.SLIDE_UP),

        // No back button
        null
      );
    });

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

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  private void initializePoseCustomize(GuiInstance<ArmorStandModel> inst, MoveablePart part) {
//    moving.put(p, new MoveRequest(part, model, props, false, null, null));
  }
}
