package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Optional;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Maps materials which can be used as a fuel source for furnaces to
  their burning time, as specified by the minecraft fandom page.
*/
@Getter
@AllArgsConstructor
public enum FuelSource {

  LAVA_BUCKET(m -> m == Material.LAVA_BUCKET ? 20000L : null),
  BLOCK_OF_COAL(m -> m == Material.COAL_BLOCK ? 16000L : null),
  DRIED_KELP_BLOCK(m -> m == Material.DRIED_KELP_BLOCK ? 4000L : null),
  BLAZE_ROD(m -> m == Material.BLAZE_ROD ? 2400L : null),
  COAL(m -> m == Material.COAL ? 1600L : null),
  CHARCOAL(m -> m == Material.CHARCOAL ? 1600L : null),
  BOAT(m -> Tag.ITEMS_BOATS.isTagged(m) ? 1200L : null),
  SCAFFOLDING(m -> m == Material.SCAFFOLDING ? 400L : null),
  BEE_NEST(m -> m == Material.BEE_NEST ? 300L : null),
  BEE_HIVE(m -> m == Material.BEEHIVE ? 300L : null),
  LOG(m -> Tag.LOGS.isTagged(m) ? 300L : null),
  PLANKS(m -> Tag.PLANKS.isTagged(m) ? 300L : null),
  SLAB(m -> Tag.SLABS.isTagged(m) ? 150L : null),
  STAIRS(m -> Tag.STAIRS.isTagged(m) ? 300L : null),
  PRESSURE_PLATE(m -> Tag.PRESSURE_PLATES.isTagged(m) ? 300L : null),
  WOODEN_BUTTON(m -> Tag.WOODEN_BUTTONS.isTagged(m) ? 100L : null),
  TRAPDOOR(m -> Tag.TRAPDOORS.isTagged(m) ? 300L : null),
  FENCE_GATE(m -> Tag.FENCE_GATES.isTagged(m) ? 300L : null),
  FENCE(m -> Tag.FENCES.isTagged(m) ? 300L : null),
  LADDER(m -> m == Material.LADDER ? 300L : null),
  CRAFTING_TABLE(m -> m == Material.CRAFTING_TABLE ? 300L : null),
  CARTOGRAPHY_TABLE(m -> m == Material.CARTOGRAPHY_TABLE ? 300L : null),
  FLETCHING_TABLE(m -> m == Material.FLETCHING_TABLE ? 300L : null),
  SMITHING_TABLE(m -> m == Material.SMITHING_TABLE ? 300L : null),
  LOOM(m -> m == Material.LOOM ? 300L : null),
  BOOKSHELF(m -> m == Material.BOOKSHELF ? 300L : null),
  LECTERN(m -> m == Material.LECTERN ? 300L : null),
  COMPOSTER(m -> m == Material.COMPOSTER ? 300L : null),
  CHEST(m -> m == Material.CHEST ? 300L : null),
  TRAPPED_CHEST(m -> m == Material.TRAPPED_CHEST ? 300L : null),
  BARREL(m -> m == Material.BARREL ? 300L : null),
  DAYLIGHT_DETECTOR(m -> m == Material.DAYLIGHT_DETECTOR ? 300L : null),
  JUKEBOX(m -> m == Material.JUKEBOX ? 300L : null),
  NOTE_BLOCK(m -> m == Material.NOTE_BLOCK ? 300L : null),
  BANNER(m -> Tag.ITEMS_BANNERS.isTagged(m) ? 300L : null),
  CROSSBOW(m -> m == Material.CROSSBOW ? 200L : null),
  BOW(m -> m == Material.BOW ? 200L : null),
  FISHING_ROD(m -> m == Material.FISHING_ROD ? 200L : null),
  WOODEN_DOOR(m -> Tag.WOODEN_DOORS.isTagged(m) ? 200L : null),
  SIGN(m -> Tag.SIGNS.isTagged(m) ? 200L : null),
  WOODEN_PICKAXE(m -> m == Material.WOODEN_PICKAXE ? 200L : null),
  WOODEN_SHOVEL(m -> m == Material.WOODEN_SHOVEL ? 200L : null),
  WOODEN_HOE(m -> m == Material.WOODEN_HOE ? 200L : null),
  WOODEN_AXE(m -> m == Material.WOODEN_AXE ? 200L : null),
  WOODEN_SWORD(m -> m == Material.WOODEN_SWORD ? 200L : null),
  BOWL(m -> m == Material.BOWL ? 100L : null),
  SAPLING(m -> Tag.SAPLINGS.isTagged(m) ? 100L : null),
  STICK(m -> m == Material.STICK ? 100L : null),
  AZALEA(m -> m == Material.AZALEA ? 100L : null),
  WOOL(m -> Tag.WOOL.isTagged(m) ? 100L : null),
  CARPET(m -> Tag.CARPETS.isTagged(m) ? 67L : null),
  BAMBOO(m -> m == Material.BAMBOO ? 50L : null),
  ;

  private final Function<Material, Long> burningTime;

  /**
   * Get the burning time of a specific material
   * @param mat Material to check
   * @return Burning time in ticks or null if this item is no fuel source
   */
  public static Optional<Long> getBurningTime(Material mat) {
    for (FuelSource fs : values()) {
      Long time = fs.burningTime.apply(mat);

      if (time == null)
        continue;

      return Optional.of(time);
    }

    return Optional.empty();
  }
}
