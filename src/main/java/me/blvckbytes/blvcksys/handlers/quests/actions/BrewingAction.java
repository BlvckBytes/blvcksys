package me.blvckbytes.blvcksys.handlers.quests.actions;

import com.google.common.collect.ImmutableList;
import me.blvckbytes.blvcksys.config.sections.QuestPotionParameterEffectSection;
import me.blvckbytes.blvcksys.config.sections.QuestPotionParameterSection;
import me.blvckbytes.blvcksys.config.sections.QuestAction;
import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.events.InventoryManipulationEvent;
import me.blvckbytes.blvcksys.events.ManipulationAction;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.handlers.quests.IQuestHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.alchemy.PotionRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/26/2022

  Listens to brewing stand inventory manipulations and remembers which items have been
  initialized by which player. If a potion finished the brewing process, it will be
  analyzed and compared against all available brewing tasks. If a task matches, it's fired.
 */
@AutoConstruct
public class BrewingAction extends AQuestAction {

  // All brewing stand potions the player initialized, as only those count
  private final Map<Player, List<ItemStack>> initializedItems;
  private final MCReflect refl;

  public BrewingAction(
    @AutoInject IQuestHandler questHandler,
    @AutoInject JavaPlugin plugin,
    @AutoInject MCReflect refl
  ) {
    super(questHandler, plugin, QuestAction.BREWING);
    this.initializedItems = new HashMap<>();
    this.refl = refl;
  }

  @EventHandler
  public void onManip(InventoryManipulationEvent e) {
    Player p = e.getPlayer();

    // The brewing stand has been modified
    if (e.getTargetInventory().getType() == InventoryType.BREWING) {
      // Put a new item in
      if (
        e.getAction() == ManipulationAction.PLACE ||
        e.getAction() == ManipulationAction.SWAP ||
        e.getAction() == ManipulationAction.MOVE
      ) {
        // Took the current item out
        if (e.getAction() == ManipulationAction.SWAP)
          unregisterInitialized(p, e.getTargetInventory().getItem(e.getTargetSlot()));

        // Register the inserted item on the next tick
        Bukkit.getScheduler().runTask(plugin, () -> {
          ItemStack item = e.getTargetInventory().getItem(e.getTargetSlot());
          if (item == null)
            return;

          if (!initializedItems.containsKey(p))
            initializedItems.put(p, new ArrayList<>());
          initializedItems.get(p).add(item);
        });
        return;
      }

      if (
        e.getAction() == ManipulationAction.PICKUP ||
        e.getAction() == ManipulationAction.DROP
      ) {
        if (initializedItems.containsKey(p))
          unregisterInitialized(p, e.getTargetInventory().getItem(e.getTargetSlot()));
      }
      return;
    }

    // The brewing stand has been modified
    if (e.getOriginInventory().getType() == InventoryType.BREWING) {
      // Moved out of the brewing stand
      if (e.getAction() == ManipulationAction.MOVE)
        unregisterInitialized(p, e.getOriginInventory().getItem(e.getOriginSlot()));
    }
  }

  @EventHandler
  public void onBrew(BrewEvent e) {
    // Get all potions within the brewing inventory with their respective slots
    List<Tuple<Integer, ItemStack>> results = new ArrayList<>();
    for (int i = 0; i < e.getContents().getSize(); i++) {
      ItemStack item = e.getContents().getItem(i);
      if (item != null && (
        item.getType() == Material.POTION ||
        item.getType() == Material.SPLASH_POTION ||
        item.getType() == Material.LINGERING_POTION
      ))
        results.add(new Tuple<>(i, item));
    }

    // Loop all known initialized items
    for (Map.Entry<Player, List<ItemStack>> initE : initializedItems.entrySet()) {

      // Loop all results
      for (Iterator<Tuple<Integer, ItemStack>> resI = results.iterator(); resI.hasNext();) {
        Tuple<Integer, ItemStack> result = resI.next();

        // The result was not initialized by the current player
        if (!initE.getValue().contains(result.b()))
          continue;

        // Remove the item from the init-list and the result to avoid dead iterations
        initE.getValue().remove(result.b());
        resI.remove();

        // Relay this event with the brewing result
        Bukkit.getScheduler().runTask(plugin, () -> {
          ItemStack item = e.getContents().getItem(result.a());
          if (item != null) {
            playerBrewed(initE.getKey(), item);

            // The result also has been initiated by this player
            if (!initializedItems.containsKey(initE.getKey()))
              initializedItems.put(initE.getKey(), new ArrayList<>());
            initializedItems.get(initE.getKey()).add(item);
          }
        });
      }
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    initializedItems.remove(e.getPlayer());
  }

  /**
   * Unregisters a previously registered item a player initialized
   * @param p Target player
   * @param item Target item
   */
  private void unregisterInitialized(Player p, ItemStack item) {
    if (initializedItems.containsKey(p)) {
      // Removed item exists
      if (item != null)
        initializedItems.get(p).remove(item);
    }
  }

  /**
   * Called whenever a player has brewed a potion
   * @param p Player which initialized the action
   * @param item Brewed item
   */
  private void playerBrewed(Player p, ItemStack item) {
    for (Map.Entry<String, QuestTaskSection> task : tasks.entrySet()) {

      // Not a valid potion task
      if (!(task.getValue().getParameters() instanceof QuestPotionParameterSection pp))
        continue;

      // Not matching this task's parameter requirements
      if (!compareResult(item, pp))
        continue;

      // Fire this task and only stop looping if it was successful
      if (questHandler.fireTask(p, task.getKey()) == TriResult.SUCC)
        break;
    }
  }

  /**
   * Compare a brewing result item with a task's parameters
   * @param item Result item
   * @param pp Task parameters
   * @return True on match, false on mismatch
   */
  private boolean compareResult(ItemStack item, QuestPotionParameterSection pp) {
    // Not a valid potion
    if (!(item.getItemMeta() instanceof PotionMeta pm))
      return false;

    // Potion type mismatch
    if (
      pp.getSplash() && item.getType() != Material.SPLASH_POTION ||
      pp.getLingering() && item.getType() != Material.LINGERING_POTION
    )
      return false;

    // No effects to compare yet
    if (pp.getEffects().length == 0)
      return false;

    boolean allMatched = true;
    for (QuestPotionParameterEffectSection effect : pp.getEffects()) {

      if (compareEffectSection(item, pm, effect)) {
        // One match is enough to succeed
        if (pp.getAnyOf())
          return true;

        // Check other entries too
        continue;
      }

      allMatched = false;

      // One mismatch broke the result
      if (!pp.getAnyOf())
        break;
    }

    return allMatched;
  }

  /**
   * Compare a potion effect section with a potion's meta and check, if it's present
   * @param pm Target potion's meta
   * @param effect Target effect
   * @return True on match, false on mismatch
   */
  private boolean compareEffectSection(ItemStack item, PotionMeta pm, QuestPotionParameterEffectSection effect) {
    // Color specified and mismatching
    if (effect.getColor() != null && !effect.getColor().equals(pm.getColor()))
      return false;

    PotionEffect baseEffect = getBaseEffect(item).orElse(null);

    // Base type matched
    if (comparePotionEffect(
      pm.getBasePotionData().getType(),
      baseEffect == null ? null : baseEffect.getType(),
      baseEffect == null ? null : baseEffect.getAmplifier(),
      baseEffect == null ? null : baseEffect.getDuration(),
      effect
    ))
      return true;

    // Check for custom effects that might match
    for (PotionEffect pe : pm.getCustomEffects()) {
      if (comparePotionEffect(null, pe.getType(), pe.getAmplifier(), pe.getDuration(), effect))
        return true;
    }

    // No matches found
    return false;
  }

  /**
   * Compares the parameters of a potion's effect with the section
   * @param type Type of potion
   * @param potionEffect Potion effect type
   * @param amplifier Amplifier of the effect
   * @param duration Duration of the effect
   * @param effect Effect section to compare against
   * @return True on match, false on mismatch
   */
  private boolean comparePotionEffect(
    @Nullable PotionType type,
    @Nullable PotionEffectType potionEffect,
    @Nullable Integer amplifier,
    @Nullable Integer duration,
    QuestPotionParameterEffectSection effect
  ) {
    if (effect.getPotionType() != null && !effect.getPotionType().equals(type))
      return false;

    if (effect.getEffectType() != null && !effect.getEffectType().equals(potionEffect))
      return false;

    if (effect.getAmplifier() != null && !effect.getAmplifier().equals(amplifier))
      return false;

    if (effect.getDuration() != null && !effect.getDuration().equals(duration))
      return false;

    return true;
  }

  /**
   * Get the base effect of a potion as a potion effect - which offers
   * all required details - through the way of NMS, as bukkit's API lacks features here
   * @param potion Target potion item stack
   * @return Potion effect on success, empty on internal errors
   */
  private Optional<PotionEffect> getBaseEffect(ItemStack potion) {
    try {
      // Get the NMS stack
      net.minecraft.world.item.ItemStack nms = (net.minecraft.world.item.ItemStack) refl.findMethodByName(
        refl.getClassBKT("inventory.CraftItemStack"),
        "asNMSCopy", ItemStack.class
      ).invoke(null, potion);

      if (nms == null)
        return Optional.empty();

      // Get the attached NBT-tags
      NBTTagCompound tag = refl.getFieldByType(nms, NBTTagCompound.class, 0);
      if (tag == null)
        return Optional.empty();

      // Find the getString method and invoke it to get the potion type
      Method getString = refl.findMethodByReturnAndArgs(NBTTagCompound.class, String.class, true, String.class).orElseThrow();
      String val = (String) getString.invoke(tag, "Potion");

      // Look up the potion's name within the potion registry
      String name = val.split(":")[1];
      Object reg = refl.findMethodByReturn(PotionRegistry.class, PotionRegistry.class)
        .orElseThrow()
        .invoke(null, name);

      // Get the effects field and make sure it's not empty
      ImmutableList<?> effs = refl.getGenericFieldByType(reg, ImmutableList.class, MobEffect.class, 0);
      if (effs.size() == 0)
        return Optional.empty();

      // Return the first effect, as there should only be one
      return Optional.of(
        (PotionEffect) refl.findMethodByName(
          refl.getClassBKT("potion.CraftPotionUtil"),
          "toBukkit", MobEffect.class
        ).invoke(null, effs.get(0))
      );
    } catch (Exception e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }
}
