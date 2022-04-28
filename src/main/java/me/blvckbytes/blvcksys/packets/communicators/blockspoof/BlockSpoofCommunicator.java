package me.blvckbytes.blvcksys.packets.communicators.blockspoof;

import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Creates all packets in regard to spoofing fake blocks for a given player.
*/
@AutoConstruct
public class BlockSpoofCommunicator implements IBlockSpoofCommunicator {

  private final MCReflect refl;
  private final ILogger logger;

  public BlockSpoofCommunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
  ) {
    this.refl = refl;
    this.logger = logger;
  }

  @Override
  public boolean spoofBlock(Player p, Location loc, Material mat) {
    return refl.createGarbageInstance(PacketPlayOutBlockChange.class)
      .map(pbc -> {
        // Set the position to the provided location
        BlockPosition pos = new BlockPosition(loc.getX(), loc.getY(), loc.getZ());
        refl.setFieldByType(pbc, BlockPosition.class, pos);

        try {
          // Create a new block from scratch using the specified material
          Class<?> cmnC = refl.getClassBKT("util.CraftMagicNumbers");
          refl.findMethodByName(cmnC, "getBlock", Material.class)
            .flatMap(m -> refl.invokeMethod(m, null, mat))
            // Get block data
            .flatMap(b -> refl.getFieldByType(b, IBlockData.class))
            .ifPresent(bd -> refl.setFieldByType(pbc, IBlockData.class, bd));
        } catch (Exception e) {
          logger.logError(e);
        }

        return pbc;
      })
      .map(pbc -> refl.sendPacket(p, pbc))
      .orElse(false);
  }
}
