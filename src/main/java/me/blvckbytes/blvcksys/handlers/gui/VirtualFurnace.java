package me.blvckbytes.blvcksys.handlers.gui;

import lombok.Getter;
import lombok.Setter;
import me.blvckbytes.blvcksys.util.MCReflect;
import net.minecraft.network.protocol.game.PacketPlayOutWindowData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Represents a virtual furnace which is in essence a small state machine that
  advances on every call to tick() and manages fueling as well as smeling. Updates
  are sent using packets to the furnace GUI at the client after every tick.
*/
public class VirtualFurnace {

  // Mapping furnace input types to their corresponding output type
  private static final Map<Material, Material> smeltingRecipes;

  // Time in ticks that one item takes to smelt
  // 200 is the default, according to protocol wiki
  // This seems to match experience, as two sticks may smelt one item
  private static final int SMELT_DURATION_T = 200;

  static {
    smeltingRecipes = new HashMap<>();

    // Load all smelting recipes in the form of I->O into the local map
    Iterator<Recipe> recipes = Bukkit.recipeIterator();
    while (recipes.hasNext()) {
      if (recipes.next() instanceof FurnaceRecipe fr)
        smeltingRecipes.put(fr.getInput().getType(), fr.getResult().getType());
    }
  }

  @Getter @Setter
  private ItemStack smelting;

  @Getter @Setter
  private ItemStack smelted;

  @Getter @Setter
  private ItemStack powerSource;

  private final Player holder;
  private int remainingBurningTime;
  private int elapsedSmeltingTime;
  private int maximumBurningTime;
  private boolean maximumBurningTimeSent;
  private boolean maximumSmeltingTimeSent;
  private Integer containerId;

  @Getter
  private final int index;

  /**
   * Create a new virtual furnace in it's default empty state for a player
   * @param holder Target player that uses this furnace
   */
  public VirtualFurnace(Player holder, int index) {
    this.holder = holder;
    this.index = index;
  }

  public void setContainerId(@Nullable Integer containerId) {
    this.containerId = containerId;

    // Reset state that's only sent initially
    if (this.containerId == null)
      maximumSmeltingTimeSent = false;
  }

  /**
   * Advances the virtual furnace's state by one tick and thus handles actions
   * like using fuel, decreasing remaining fuel time and advancing the smelting process.
   * @param refl MCReflect ref for sending packets
   */
  public void tick(MCReflect refl) {
    // Synchronize window
    if (containerId != null)
      this.syncWindow(containerId, refl);

    // Decrease the burning time
    remainingBurningTime = Math.max(0, remainingBurningTime - 1);

    // Nothing to smelt
    if (smelting == null) {
      elapsedSmeltingTime = 0;
      return;
    }

    Material resultType = smeltingRecipes.get(smelting.getType());

    // Cannot smelt this type of material
    if (resultType == null) {
      elapsedSmeltingTime = 0;
      return;
    }

    ItemStack result = new ItemStack(resultType);

    // Cannot stack the result with whatever's in the smelted slot
    if (smelted != null && (!smelted.isSimilar(result) || smelted.getAmount() == smelted.getMaxStackSize())) {
      elapsedSmeltingTime = 0;
      return;
    }

    // No more burning time available
    if (remainingBurningTime <= 0) {

      // There's a power source inserted
      ItemStack source = getPowerSource();
      if (source != null) {

        // Check if it is valid fuel and get it's burning time
        int burningTime = FuelSource.getBurningTime(source.getType()).orElse(0L).intValue();

        if (burningTime > 0) {
          // Update the burning time
          remainingBurningTime = burningTime;
          maximumBurningTime = burningTime;
          maximumBurningTimeSent = false;

          // Decrease the source amount
          source.setAmount(source.getAmount() - 1);
          if (source.getAmount() == 0)
            setPowerSource(null);
        }

        // Item cannot deliver any power
        else {
          elapsedSmeltingTime = 0;
          return;
        }
      }

      // Power source slot is empty
      else {
        elapsedSmeltingTime = 0;
        return;
      }
    }

    // Item smelted fully
    if (++elapsedSmeltingTime == SMELT_DURATION_T) {
      elapsedSmeltingTime = 0;

      // Decrease the smelting stack
      smelting.setAmount(smelting.getAmount() - 1);
      if (smelting.getAmount() == 0)
        smelting = null;

      // Intially set the smelted stack
      if (smelted == null)
        smelted = result;

      // Increase the smelted stack
      else
        smelted.setAmount(smelted.getAmount() + 1);
    }
  }

  /**
   * Synchronize the corresponding window at the client by sending window data packets
   * in order to fully communicate the current state of this virtual furnace.
   * @param containerId ID of the container used to display this furnace
   * @param refl MCReflect ref for sending packets
   */
  private void syncWindow(int containerId, MCReflect refl) {
    // Send the fuel left status
    // 0: Fire icon (fuel left) counting from fuel burn time down to 0 (in-game ticks)
    refl.sendPacket(
      holder, new PacketPlayOutWindowData(containerId, 0, remainingBurningTime)
    );

    // Maximum burning time hasn't yet been announced
    // 1: Maximum fuel burn time fuel burn time or 0 (in-game ticks)
    if (!maximumBurningTimeSent) {
      maximumBurningTimeSent = true;
      refl.sendPacket(
        holder, new PacketPlayOutWindowData(containerId, 1, maximumBurningTime)
      );
    }

    // Maximum smelting time hasn't yet been announced
    // 3: Maximum progress always 200 on the notchian server
    if (!maximumSmeltingTimeSent) {
      maximumSmeltingTimeSent = true;
      refl.sendPacket(
        holder, new PacketPlayOutWindowData(containerId, 3, SMELT_DURATION_T)
      );
    }

    // 2: Progress arrow counting from 0 to maximum progress (in-game ticks)
    refl.sendPacket(
      holder, new PacketPlayOutWindowData(containerId, 2, elapsedSmeltingTime)
    );
  }
}