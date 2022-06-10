package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.commands.IGiveCommand;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.events.ManipulationAction;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/10/2022

  Create a new auction using a "wizard".
*/
@AutoConstruct
public class AHCreateGui extends AGui<Object> {

  // GUI state container, used per player
  @AllArgsConstructor
  private static class AHCreateState {
    int startBid;
    int durationSeconds;
    @Nullable ItemStack item;

    private static AHCreateState makeDefault() {
      return new AHCreateState(100, 60 * 60, null);
    }
  }

  // Maximum duration in seconds an auction may be available for
  private static final int MAX_DURATION_S = 60 * 60 * 72;

  @AutoInjectLate
  private AHProfileGui ahProfileGui;
  private final TimeUtil timeUtil;
  private final ChatUtil chatUtil;
  private final IGiveCommand giveCommand;
  private final Map<Player, AHCreateState> states;

  public AHCreateGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject TimeUtil timeUtil,
    @AutoInject ChatUtil chatUtil,
    @AutoInject IGiveCommand giveCommand
  ) {
    super(3, "", i -> (
      cfg.get(ConfigKey.GUI_CREATE_AH)
    ), plugin, cfg, textures);

    this.states = new HashMap<>();

    this.timeUtil = timeUtil;
    this.chatUtil = chatUtil;
    this.giveCommand = giveCommand;
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    Player p = inst.getViewer();
    AHCreateState state = getState(inst);

    if (state.item != null) {
      giveCommand.giveItemsOrDrop(p, state.item);
      state.item = null;

      p.sendMessage(
        cfg.get(ConfigKey.GUI_CREATE_AH_ITEM_RETURNED)
          .withPrefix()
          .asScalar()
      );
    }

    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);
    inst.addBack(18, ahProfileGui, null, AnimationType.SLIDE_RIGHT);

    // Auction item markers
    inst.fixedItem("4,22", () -> (
      new ItemStackBuilder(Material.PURPLE_STAINED_GLASS_PANE)
        .withName(ConfigValue.immediate(" "))
        .build()
    ), null);

    // Start bid amount
    inst.fixedItem(11, () -> (
      new ItemStackBuilder(Material.GOLD_INGOT)
        .withName(cfg.get(ConfigKey.GUI_CREATE_AH_START_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_CREATE_AH_START_LORE)
            .withVariable("start_bid", getState(inst).startBid)
        )
        .build()
    ), e -> {
      new UserInputChain(inst, values -> {
        // Cannot request negative or zero amounts
        int amount = (int) values.get("startBid");
        if (amount <= 0) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_CREATE_AH_START_PROMPT_INVALID)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        // Update start bid and notify
        getState(inst).startBid = amount;
        p.sendMessage(
          cfg.get(ConfigKey.GUI_CREATE_AH_START_PROMPT_SUCCESS)
            .withPrefix()
            .withVariable("amount", amount)
            .asScalar()
        );
      }, null, chatUtil)
        .withPrompt(
          "startBid",
          values -> cfg.get(ConfigKey.GUI_CREATE_AH_START_PROMPT_MESSAGE),
          // Has to be an integer number
          Integer::parseInt,
          input -> cfg.get(ConfigKey.ERR_INTPARSE)
            .withPrefix()
            .withVariable("number", input),
          null
        )
        .start();
    });

    // Auction item
    inst.fixedItem(13, () -> {
      AHCreateState state = getState(inst);

      // Decide whether to render either the placeholder or the auction's item
      return new ItemStackBuilder(
        state.item == null ? new ItemStack(Material.BARRIER) : state.item,
        state.item == null ? 1 : state.item.getAmount()
      )
        .withName(cfg.get(ConfigKey.GUI_CREATE_AH_ITEM_NAME), () -> state.item == null)
        .withLore(
          state.item == null ?
            cfg.get(ConfigKey.GUI_CREATE_AH_ITEM_LORE_VACANT) :
            cfg.get(ConfigKey.GUI_CREATE_AH_ITEM_LORE_CHOSEN)
        )
        .build();
    }, e -> {
      AHCreateState state = getState(inst);

      // Tries to put a new item into the slot
      if (e.getAction() == ManipulationAction.SWAP) {
        ItemStack item = p.getItemOnCursor();

        // Had no item on the cursor (should never happen)
        if (item.getType().isAir())
          return;

        // Had no item selected yet
        if (state.item == null) {
          // Store the item in local state and clear the cursor
          state.item = item.clone();
          p.setItemOnCursor(null);
        }

        // Wants to swap out the selected item with another item
        else {
          p.setItemOnCursor(state.item);
          state.item = item;
        }

        inst.redraw("13");
      }

      // Tries to pick up an item from the slot
      if (e.getAction() == ManipulationAction.PICKUP) {
        // Has no item set yet
        if (state.item == null)
          return;

        // Hand the item back to the cursor and clear the state item
        p.setItemOnCursor(state.item);
        state.item = null;
        inst.redraw("13");
      }
    });

    // Auction duration
    inst.fixedItem(15, () -> (
      new ItemStackBuilder(Material.CLOCK)
        .withName(cfg.get(ConfigKey.GUI_CREATE_AH_DURATION_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_CREATE_AH_DURATION_LORE)
            .withVariable("duration", timeUtil.formatDurationHHCMM(getState(inst).durationSeconds))
        )
        .build()
    ), e -> {
      ClickType click = e.getClick();

      if (!(click.isLeftClick() || click.isRightClick()))
        return;

      AHCreateState state = getState(inst);

      // Shift changes minutes
      if (e.getClick().isShiftClick())
        state.durationSeconds += 60 * 10 * (e.getClick().isRightClick() ? -1 : 1);

      // Non-shift changes hours
      else
        state.durationSeconds += 60 * 60 * (e.getClick().isRightClick() ? -1 : 1);

      // Constrain towards zero and max duration
      state.durationSeconds = Math.max(0, Math.min(MAX_DURATION_S, state.durationSeconds));

      inst.redraw("15");
    });

    return true;
  }

  /**
   * Either gets the cached state or creates a new default instance
   * @param inst GUI ref
   * @return State of the GUI's viewer
   */
  private AHCreateState getState(GuiInstance<Object> inst) {
    if (states.containsKey(inst.getViewer()))
      return states.get(inst.getViewer());
    AHCreateState state = AHCreateState.makeDefault();
    states.put(inst.getViewer(), state);
    return state;
  }
}
