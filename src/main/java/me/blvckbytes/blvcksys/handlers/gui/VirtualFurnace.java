package me.blvckbytes.blvcksys.handlers.gui;

import lombok.Getter;
import lombok.Setter;
import me.blvckbytes.blvcksys.util.MCReflect;
import net.minecraft.network.protocol.game.PacketPlayOutWindowData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Represents a virtual furnace which is in essence a small state machine that
  advances on every call to tick() and manages fueling as well as smeling. Updates
  are sent using packets to the furnace GUI at the client after every tick.
*/
@Setter
@Getter
public class VirtualFurnace {

  private Player holder;
  private ItemStack smelting;
  private ItemStack smelted;
  private ItemStack powerSource;
  private long remainingBurningTime;
  private long maximumBurningTime;
  private boolean maximumBurningTimeSent;

  /**
   * Create a new virtual furnace in it's default empty state for a player
   * @param holder Target player that uses this furnace
   */
  public VirtualFurnace(Player holder) {
    this.holder = holder;
    this.smelting = null;
    this.smelted = null;
    this.powerSource = null;
    this.remainingBurningTime = 0;
    this.maximumBurningTime = 0;
    this.maximumBurningTimeSent = false;
  }

  /**
   * Advances the virtual furnace's state by one tick and thus handles actions
   * like using fuel, decreasing remaining fuel time and advancing the smelting process.
   * @param containerId ID of the container used to display this furnace
   * @param refl MCReflect ref for sending packets
   */
  public void tick(int containerId, MCReflect refl) {
    // Decrease the burning time
    setRemainingBurningTime(Math.max(0, getRemainingBurningTime() - 1));

    // No more burning time available
    if (getRemainingBurningTime() == 0) {

      // There's a power source inserted
      ItemStack source = getPowerSource();
      if (source != null) {

        // Check if it is valid fuel and get it's burning time
        FuelSource.getBurningTime(source.getType())
          .ifPresent(burningTime -> {

            // Update the burning time
            setRemainingBurningTime(burningTime);
            setMaximumBurningTime(burningTime);
            setMaximumBurningTimeSent(false);

            // Decrease the amount by one
            int amount = source.getAmount();
            source.setAmount(amount - 1);

            // Stack is empty, remove from model
            if (amount == 1)
              setPowerSource(null);
          });
      }
    }

    // Keep the local state in sync with the open window's state
    this.syncWindow(containerId, refl);
  }

  /**
   * Synchronize the corresponding window at the client by sending window data packets
   * in order to fully communicate the current state of this virtual furnace.
   * @param containerId ID of the container used to display this furnace
   * @param refl MCReflect ref for sending packets
   */
  private void syncWindow(int containerId, MCReflect refl) {
    // Send the fuel left status
    // 0: Fire icon (fuel left)	counting from fuel burn time down to 0 (in-game ticks)
    refl.sendPacket(
      holder, new PacketPlayOutWindowData(containerId, 0, (int) getRemainingBurningTime())
    );

    // Maximum burning time hasn't yet been announced
    // 1: Maximum fuel burn time	fuel burn time or 0 (in-game ticks)
    if (!isMaximumBurningTimeSent()) {
      setMaximumBurningTimeSent(true);
      refl.sendPacket(
        holder, new PacketPlayOutWindowData(containerId, 1, (int) getMaximumBurningTime())
      );
    }

    /*
      TODO: Implement
      2: Progress arrow	counting from 0 to maximum progress (in-game ticks)
      3: Maximum progress	always 200 on the notchian server
     */
  }
}