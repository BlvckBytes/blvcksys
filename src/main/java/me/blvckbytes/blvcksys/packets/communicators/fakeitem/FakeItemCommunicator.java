package me.blvckbytes.blvcksys.packets.communicators.fakeitem;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/30/2022

  Creates all packets in regard to setting fake items in the player's inventory.
*/
@AutoConstruct
public class FakeItemCommunicator implements IFakeItemCommunicator {

  private final MCReflect refl;
  private final ILogger logger;

  public FakeItemCommunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
  ) {
    this.refl = refl;
    this.logger = logger;
  }

  @Override
  public boolean setFakeSlot(Player p, @Nullable ItemStack is, int slot) {
    // Invalid slot
    if (slot < 0 || slot > 35)
      return false;

    // Create slot setting packet to move this fake book into the inventory
    try {
      Object poss = refl.createPacket(PacketPlayOutSetSlot.class);

      refl.setFieldByType(poss, int.class, -2, 0); // Window ID (-2=inv)
      refl.setFieldByType(poss, int.class, 0, 1); // State ID (leave at zero for now)
      refl.setFieldByType(poss, int.class, slot, 2); // Slot

      if (is == null)
        is = new ItemStack(Material.AIR);

      // Convert the bukkit item stack to a craft item stack and set the corresponding field
      Class<?> cisC = refl.getClassBKT("inventory.CraftItemStack");
      Object cis = refl.findMethodByName(cisC, "asNMSCopy", ItemStack.class).invoke(null, is);
      refl.setFieldByType(poss, net.minecraft.world.item.ItemStack.class, cis, 0);

      return refl.sendPacket(p, poss);
    } catch (Exception e) {
      logger.logError(e);
      return false;
    }
  }
}
