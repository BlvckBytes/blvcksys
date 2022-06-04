package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.EquipmentSlot;
import me.blvckbytes.blvcksys.handlers.IArmorStandHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.MoveablePart;
import me.blvckbytes.blvcksys.packets.communicators.armorstand.ArmorStandProperties;
import me.blvckbytes.blvcksys.persistence.models.ArmorStandModel;
import me.blvckbytes.blvcksys.util.ChatButtons;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.SymbolicHead;
import me.blvckbytes.blvcksys.util.Triple;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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
  private final ItemEditorGui itemEditorGui;
  private final ChatUtil chatUtil;

  public ArmorStandGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IArmorStandHandler standHandler,
    @AutoInject ItemEditorGui itemEditorGui,
    @AutoInject ChatUtil chatUtil
  ) {
    super(6, "", i -> (
      cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_NAME).
        withVariable("name", i.getArg().getName())
    ), plugin, cfg, textures);

    this.moving = new HashMap<>();
    this.standHandler = standHandler;
    this.chatUtil = chatUtil;
    this.itemEditorGui = itemEditorGui;
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
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.LETTER_B.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_CHESTPLATE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_CHESTPLATE_LORE))
        .build()
    ), null);

    inst.fixedItem(12, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.LETTER_H.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_LEGGINGS_NAME))
        .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_LEGGINGS_LORE))
        .build()
    ), null);

    inst.fixedItem(13, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.LETTER_S.getOwner()))
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

    equipmentSlotTemplate(inst, 19, EquipmentSlot.HELMET, props);
    equipmentSlotTemplate(inst, 20, EquipmentSlot.CHESTPLATE, props);
    equipmentSlotTemplate(inst, 21, EquipmentSlot.LEGGINGS, props);
    equipmentSlotTemplate(inst, 22, EquipmentSlot.BOOTS, props);
    equipmentSlotTemplate(inst, 23, EquipmentSlot.OFF_HAND, props);
    equipmentSlotTemplate(inst, 24, EquipmentSlot.MAIN_HAND, props);

    /////////////////////////////////// Body Poses ////////////////////////////////////

    poseSlotTemplate(inst, 28, MoveablePart.HEAD, props);
    poseSlotTemplate(inst, 29, MoveablePart.BODY, props);
    poseSlotTemplate(inst, 30, MoveablePart.LEFT_LEG, props);
    poseSlotTemplate(inst, 31, MoveablePart.RIGHT_LEG, props);
    poseSlotTemplate(inst, 32, MoveablePart.LEFT_ARM, props);
    poseSlotTemplate(inst, 33, MoveablePart.RIGHT_ARM, props);

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

    /*
      -90 -> 0 -> 90 (OK)
      0 -> -180 -> 180 -> 0 (n. OK), |a| => 0 -> 180 -> 0 (OK)

      Since I calculate deltas between moves, I want yaw and pitch to
      not have any jumps like the one from -180 to 180, because that would drive
      the delta to a non-matching, high amount.
     */

    // Calculate movement delta and update the previous location
    Location nextLoc = p.getLocation().clone();
    double dYaw = Math.abs(nextLoc.getYaw()) - Math.abs(req.prevLoc.getYaw());
    double dPitch = nextLoc.getPitch() - req.prevLoc.getPitch();
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

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    moving.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Prompt for an itemstack by telling the user to hold it in their hand while submitting the prompt
   * @param inst GUI instance
   * @param submitted Submitted item
   */
  private void promptForItem(GuiInstance<ArmorStandModel> inst, EquipmentSlot slot, Consumer<ItemStack> submitted) {
    // Close the gui and start a new chat prompt to end or cancel the session
    inst.close();

    promptForDone(
      inst,
      cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EQUIPMENT_PROMPT)
        .withVariable("slot", formatConstant(slot.name()))
        .withPrefix(),
      () -> {
        ItemStack item = inst.getViewer().getInventory().getItemInMainHand();

        // Has to have something in their hand
        if (item.getType() == Material.AIR) {
          inst.getViewer().sendMessage(
            cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EQUIPMENT_NONE)
              .withPrefix()
              .asScalar()
          );

          return;
        }

        submitted.accept(item);

        inst.getViewer().sendMessage(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EQUIPMENT_CHANGED)
            .withPrefix()
            .withVariable("slot", formatConstant(slot.name()))
            .asScalar()
        );
      }, null
    );
  }

  /**
   * Initializes the process of customizing a body part pose using mouse movement
   * @param inst GUI instance
   * @param props Current properties ref
   * @param part Body part to move
   */
  private void initializePoseCustomize(GuiInstance<ArmorStandModel> inst, ArmorStandProperties props, MoveablePart part) {
    ArmorStandProperties alterable = props.clone();

    // Close the gui and start a new chat prompt to end or cancel the session
    inst.close();

    promptForDone(
      inst,
      cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_POSE_PROMPT)
        .withPrefix()
        .withVariable("bodypart", formatConstant(part.name())),

      // Done, save the updates
      () -> {
        // Copy over the value from the clone
        part.set(props, part.get(alterable));
        standHandler.setProperties(inst.getArg().getName(), props, true);

        inst.getViewer().sendMessage(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_POSE_CHANGED)
            .withPrefix()
            .withVariable("bodypart", formatConstant(part.name()))
            .asScalar()
        );

        moving.remove(inst.getViewer());
      },

      // Cancel, restore the old state
      () -> {
        standHandler.setProperties(inst.getArg().getName(), props, false);
        moving.remove(inst.getViewer());
      }
    );

    moving.put(inst.getViewer(), new MoveRequest(part, inst.getArg(), alterable, false, null, null));
  }

  /**
   * Get an item's name by either it's item meta or by transforming
   * the material type to a human readable string
   * @param item Item to get the name for
   * @return Name to display
   */
  public ConfigValue getItemName(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    String metaName = meta == null ? null : meta.getDisplayName();

    return (metaName != null && !metaName.isBlank()) ? ConfigValue.immediate(metaName) :
      cfg.get(ConfigKey.GUI_CRATE_CONTENT_CONTENT_NAME)
        .withVariable(
          "hr_type",
          WordUtils.capitalizeFully(item.getType().name().replace("_", " "))
        );
  }

  /**
   * Item button template for an equipment slot manipulator
   * @param inst GUI instance
   * @param invSlot Slot within the GUI
   * @param slot Slot of the armorstand to manipulate
   * @param props Current properties value
   */
  private void equipmentSlotTemplate(GuiInstance<ArmorStandModel> inst, int invSlot, EquipmentSlot slot, ArmorStandProperties props) {
    inst.fixedItem(invSlot, () -> (
      slot.get(props) == null ?
        new ItemStackBuilder(Material.BARRIER)
          .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_NAME))
          .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_EMPTY_EQUIP_LORE))
          .build() :
        new ItemStackBuilder(slot.get(props), slot.get(props).getAmount())
          .withName(
            cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_USED_EQUIP_NAME)
              .withVariable("name", getItemName(slot.get(props)))
          )
          .withLore(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_USED_EQUIP_LORE))
          .build()
    ), e -> {
      if (slot.get(props) == null) {
        promptForItem(inst, slot, item -> {
          slot.set(props, item);
          standHandler.setProperties(inst.getArg().getName(), props, true);
        });
        return;
      }

      if (e.getClick().isRightClick()) {
        slot.set(props, null);
        standHandler.setProperties(inst.getArg().getName(), props, true);
        inst.redraw(String.valueOf(invSlot));
        return;
      }

      if (e.getClick().isLeftClick()) {
        inst.switchTo(AnimationType.SLIDE_LEFT, itemEditorGui, new Triple<>(
          slot.get(props),
          stack -> {
            slot.set(props, stack);
            standHandler.setProperties(inst.getArg().getName(), props, true);
            inst.redraw(String.valueOf(invSlot));
          },
          editorInst -> inst.reopen(AnimationType.SLIDE_RIGHT, editorInst)
        ));
      }
    });
  }

  /**
   * Pose button template for a pose manipulator
   * @param inst GUI instance
   * @param invSlot Slot within the GUI
   * @param part Bodypart to manipulate
   * @param props Current properties value
   */
  private void poseSlotTemplate(GuiInstance<ArmorStandModel> inst, int invSlot, MoveablePart part, ArmorStandProperties props) {
    inst.fixedItem(invSlot, () -> (
      new ItemStackBuilder(Material.GOLDEN_AXE)
        .hideAttributes()
        .withName(cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AS_CUSTOMIZE_ALTER_POSE_LORE)
            .withVariable("bodypart", formatConstant(part.name()))
        )
        .build()
    ), e -> initializePoseCustomize(inst, props, part));
  }

  /**
   * Creates a new cancel/done prompt within the chat
   * @param inst GUI instance
   * @param prefix Text to prefix to the prompt buttons
   * @param done Called when done has been pressed
   * @param cancel Called when cancel has been pressed
   */
  private void promptForDone(GuiInstance<ArmorStandModel> inst, ConfigValue prefix, Runnable done, @Nullable Runnable cancel) {
    chatUtil.sendButtons(
      inst.getViewer(),
      new ChatButtons(prefix.asScalar(), true, plugin, cfg, null)
        .addButton(cfg.get(ConfigKey.CHATBUTTONS_DONE), () -> {
          done.run();
          inst.reopen(AnimationType.SLIDE_UP);
        })

        // Re-open the GUI on cancellation
        .addButton(cfg.get(ConfigKey.CHATBUTTONS_CANCEL), () -> {
          if (cancel != null)
            cancel.run();
          inst.reopen(AnimationType.SLIDE_UP);
        })
    );
  }
}
