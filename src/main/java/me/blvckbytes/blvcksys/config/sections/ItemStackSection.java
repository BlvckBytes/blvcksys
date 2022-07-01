package me.blvckbytes.blvcksys.config.sections;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import net.minecraft.util.Tuple;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Represents the properties of a fully describeable item stack.
*/
@Getter
public class ItemStackSection extends AConfigSection {

  private @Nullable Integer amount;
  private @Nullable ConfigValue type;
  private @Nullable ConfigValue name;
  private @Nullable ConfigValue lore;
  private @Nullable ConfigValue flags;
  private @Nullable ConfigValue color;
  private ItemStackEnchantmentSection[] enchantments;
  private @Nullable GameProfile textures;
  private @Nullable ItemStackBaseEffectSection baseEffect;
  private ItemStackCustomEffectSection[] customEffects;

  public ItemStackSection() {
    this.enchantments = new ItemStackEnchantmentSection[0];
    this.customEffects = new ItemStackCustomEffectSection[0];
  }

  /**
   * Create an item stack builder from the parameters of this section
   * @param variables Variables to apply while evaluating values
   */
  public ItemStackBuilder asItem(@Nullable Map<String, String> variables) {
    Material m = getType() == null ? null : getType().copy().withVariables(variables).asScalar(Material.class);
    Color c = getColor() == null ? null : getColor().copy().withVariables(variables).asScalar(Color.class);

    return new ItemStackBuilder(
      m == null ? Material.BARRIER : m,
      amount == null ? 1 : amount
    )
      .withName(() -> name.copy().withVariables(variables), name != null)
      .withLore(() -> lore.copy().withVariables(variables), lore != null)
      .withFlags(() -> flags.copy().withVariables(variables).asList(ItemFlag.class), flags != null)
      .withEnchantments(() -> (
        Arrays.stream(enchantments)
          .map(es -> (
            new Tuple<>(
              es.getEnchantment() == null ?
                null :
                es.getEnchantment().copy().withVariables(variables).asScalar(Enchantment.class),
              es.getLevel() == null ? 1 : es.getLevel()
            )
          ))
          .filter(t -> t.a() != null)
          .toList()
      ), enchantments != null)
      .withColor(() -> c, c != null)
      .withProfile(() -> textures, textures != null)
      .withBaseEffect(() -> baseEffect.asData(variables), baseEffect != null)
      .withCustomEffects(() -> (
        Arrays.stream(customEffects)
          .map(effect -> effect.asEffect(variables).orElse(null))
          .filter(Objects::nonNull)
          .toList()
      ), customEffects != null);
  }

  /**
   * Compares all available values of this section against the
   * provided item and checks if they match
   * @param item Target item
   */
  public boolean describesItem(@Nullable ItemStack item) {
    if (item == null)
      return false;

    Material m = getType() == null ? null : getType().asScalar(Material.class);
    if (m != null && item.getType() != m)
      return false;

    if (amount != null && item.getAmount() != amount)
      return false;

    // Compare displayname
    if (!checkMeta(item, name, meta -> name.asScalar().equals(meta.getDisplayName())))
      return false;

    // Compare lore lines for equality (and order)
    if (!checkMeta(item, lore, meta -> lore.asList().equals(meta.getLore())))
      return false;

    // Compare flag entries for equality (ignoring order)
    if (!checkMeta(item, flags, meta -> flags.asSet(ItemFlag.class).equals(meta.getItemFlags())))
      return false;

    // Compare either potion color or leather color
    if (!checkMeta(item, color, meta -> {
      if (meta instanceof PotionMeta pm)
        return color.equals(pm.getColor());

      if (meta instanceof LeatherArmorMeta lam)
        return color.equals(lam.getColor());

      // Not colorable
      return false;
    }))
      return false;

    // Check for the presence of all enchantments at the right levels (ignoring order)
    if (!checkMeta(item, enchantments, meta -> {
      for (ItemStackEnchantmentSection ench : enchantments) {
        Enchantment e = ench.getEnchantment() == null ? null : ench.getEnchantment().asScalar(Enchantment.class);

        // Cannot compare
        if (e == null)
          continue;

        if (!(
          // Contains this enchantment at any levej
          meta.hasEnchant(e) &&
          // Contains at a matching level, if required
          (ench.getLevel() == null || meta.getEnchantLevel(e) == ench.getLevel())
        ))
          return false;
      }
      // All enchantments matched
      return true;
    }))
      return false;

    // Compare for head textures
    if (!checkMeta(item, textures, meta -> {
      // Not a skull
      if (!(meta instanceof SkullMeta sm))
        return false;

      OfflinePlayer owner = sm.getOwningPlayer();

      // Has no head owner
      if (owner == null)
        return null;

      return textures.getId().equals(owner.getUniqueId());
    }))
      return false;

    // Compare the base potion effect
    if (!checkMeta(item, baseEffect, meta -> {
      // Not a potion
      if (!(meta instanceof PotionMeta pm))
        return false;
      return baseEffect.describesData(pm.getBasePotionData());
    }))
      return false;

    // Check for the presence of all custom effects (ignoring order)
    if (!checkMeta(item, customEffects, meta -> {
      if (!(meta instanceof PotionMeta pm))
        return false;

      for (ItemStackCustomEffectSection eff : customEffects) {
        // Current custom effect is not represented within the custom effects of the potion
        if (pm.getCustomEffects().stream().anyMatch(eff::describesEffect))
          return false;
      }

      // All effects present
      return true;
    }))
      return false;

    // All checks passed
    return true;
  }

  /**
   * Check a local parameter against the item's metadata. Whenever the
   * local value is null, this function returns true, and if it's present
   * but there's no metadata or the metadata property mismatches, it returns false
   * @param item Item to check the meta of
   * @param value Local parameter to use
   * @param checker Checker function
   */
  private boolean checkMeta(ItemStack item, @Nullable Object value, Function<ItemMeta, Boolean> checker) {
    // Value not present, basically a wildcard
    if (value == null)
      return true;

    // Fails if there is either no meta to compare against at all or if the checker failed
    return item.getItemMeta() != null && checker.apply(item.getItemMeta());
  }
}
