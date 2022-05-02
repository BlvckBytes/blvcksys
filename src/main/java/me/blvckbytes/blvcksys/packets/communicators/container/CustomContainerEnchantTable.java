package me.blvckbytes.blvcksys.packets.communicators.container;

import me.blvckbytes.blvcksys.util.MCReflect;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerEnchantTable;
import net.minecraft.world.inventory.ContainerProperty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.WeightedRandomEnchant;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;

import java.util.List;
import java.util.Random;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Patches the vanilla calls to ContainerEnchantTable to spoof the number of
  bookshelves which would otherwise get fetched by real chunk-data, also
  wraps calls to version-depending import members using reflect.
*/
public class CustomContainerEnchantTable extends ContainerEnchantTable {

  // Number of spoofed bookshelves used when calculating enchantments
  private static final int NUM_SPOOFED_BOOKSHELVES = 15;

  private final MCReflect refl;

  public CustomContainerEnchantTable(int i, PlayerInventory playerinventory, ContainerAccess containeraccess, MCReflect refl) {
    super(i, playerinventory, containeraccess);
    this.refl = refl;
  }

  @Override
  public void a(IInventory iinventory) {
    // Try to invoke the patched method
    try {
      aPatched(iinventory);
    }

    // Or just call the vanilla impl
    catch (Exception e) {
      e.printStackTrace();
      super.a(iinventory);
    }
  }

  @SuppressWarnings("unchecked")
  private void aPatched(IInventory iinventory) throws Exception {

    // Get all hidden dependencies from the superclass
    IInventory n = refl.getFieldByType(this, IInventory.class, 0);
    ContainerAccess o = refl.getFieldByType(this, ContainerAccess.class, 0);
    Random p = refl.getFieldByType(this, Random.class, 0);
    ContainerProperty q = refl.getFieldByType(this, ContainerProperty.class, 0);
    Player player = refl.getFieldByType(this, Player.class, 0);

    if (iinventory == n) {
      ItemStack itemstack = iinventory.a(0);
      if (!itemstack.b()) {
        p.setSeed(q.b());

        int j;
        for(j = 0; j < 3; ++j) {
          this.k[j] = EnchantmentManager.a(p, j, NUM_SPOOFED_BOOKSHELVES, itemstack);
          this.l[j] = -1;
          this.m[j] = -1;
          if (this.k[j] < j + 1) {
            this.k[j] = 0;
          }
        }

        for(j = 0; j < 3; ++j) {
          if (this.k[j] > 0) {
            List<WeightedRandomEnchant> list = (List<WeightedRandomEnchant>) refl.invokeMethodByArgsOnly(
              this, new Class[] { ItemStack.class, int.class, int.class },
              itemstack, j, this.k[j]
            );
            if (list != null && !list.isEmpty()) {
              WeightedRandomEnchant weightedrandomenchant = list.get(p.nextInt(list.size()));
              this.l[j] = IRegistry.V.a(weightedrandomenchant.a);
              this.m[j] = weightedrandomenchant.b;
            }
          }
        }

        org.bukkit.inventory.ItemStack item = (org.bukkit.inventory.ItemStack) refl.findMethodByName(
          refl.getClassBKT("inventory.CraftItemStack"),
          "asCraftMirror", ItemStack.class
        ).invoke(null, itemstack);

        EnchantmentOffer[] offers = new EnchantmentOffer[3];

        for(j = 0; j < 3; ++j) {
          NamespacedKey key = (NamespacedKey) refl.findMethodByName(
            refl.getClassBKT("util.CraftNamespacedKey"),
            "fromMinecraft", MinecraftKey.class
          ).invoke(null, IRegistry.V.b(IRegistry.V.a(this.l[j])));

          Enchantment enchantment = this.l[j] >= 0 ? Enchantment.getByKey(key) : null;
          offers[j] = enchantment != null ? new EnchantmentOffer(enchantment, this.m[j], this.k[j]) : null;
        }

        PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(player, this.getBukkitView(), o.getLocation().getBlock(), item, offers, NUM_SPOOFED_BOOKSHELVES);
        event.setCancelled(!itemstack.B());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
          for(j = 0; j < 3; ++j) {
            EnchantmentOffer offer = event.getOffers()[j];
            if (offer != null) {
              this.k[j] = offer.getCost();

              MinecraftKey key = (MinecraftKey) refl.findMethodByName(
                refl.getClassBKT("util.CraftNamespacedKey"),
                "toMinecraft", NamespacedKey.class
              ).invoke(null, offer.getEnchantment().getKey());

              this.l[j] = IRegistry.V.a(IRegistry.V.a(key));
              this.m[j] = offer.getEnchantmentLevel();
            } else {
              this.k[j] = 0;
              this.l[j] = -1;
              this.m[j] = -1;
            }
          }

          this.d();
        } else {
          for(j = 0; j < 3; ++j) {
            this.k[j] = 0;
            this.l[j] = -1;
            this.m[j] = -1;
          }

        }
      } else {
        for(int i = 0; i < 3; ++i) {
          this.k[i] = 0;
          this.l[i] = -1;
          this.m[i] = -1;
        }
      }
    }
  }
}
