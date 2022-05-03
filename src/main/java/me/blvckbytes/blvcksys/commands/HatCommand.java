package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.world.item.ItemArmor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/03/2022

  Place a wearable item from your main hand onto your head.
 */
@AutoConstruct
public class HatCommand extends APlayerCommand {

  private final IGiveCommand give;

  public HatCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IGiveCommand give
  ) {
    super(
      plugin, logger, cfg, refl,
      "hat",
      "Wear an item as a hat",
      PlayerPermission.HAT
    );

    this.give = give;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    ItemStack hat = p.getInventory().getHelmet();
    ItemStack hand = p.getInventory().getItemInMainHand();

    // Has an active hat, undress
    if (hat != null && hat.getType() != Material.AIR) {
      p.sendMessage(
        cfg.get(ConfigKey.HAT_UNDRESSED)
          .withPrefix()
          .asScalar()
      );

      this.give.giveItemsOrDrop(p, hat);
      p.getInventory().setHelmet(null);
      return;
    }

    // Has to have something in their hand
    if (hand.getType() == Material.AIR) {
      p.sendMessage(
        cfg.get(ConfigKey.HAT_NO_ITEM)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    ItemMeta meta = hand.getItemMeta();
    String item = (meta == null || meta.getDisplayName().isBlank()) ? hand.getType().toString() : meta.getDisplayName();
    Optional<Object> nmsHand = refl.getNMSItem(hand);

    // Has to be a wearable item
    if (!(
      // Is a solid
      hand.getType().isSolid() ||

      // Is a pice of armor
      (nmsHand.isPresent() && nmsHand.get() instanceof ItemArmor)
    )) {
      p.sendMessage(
        cfg.get(ConfigKey.HAT_UNWEARABLE)
          .withPrefix()
          .withVariable("item", item)
          .asScalar()
      );
      return;
    }

    // Set item as hat
    p.getInventory().setHelmet(hand);
    p.getInventory().setItemInMainHand(null);

    p.sendMessage(
      cfg.get(ConfigKey.HAT_DRESSED)
        .withPrefix()
        .withVariable("item", item)
        .asScalar()
    );
  }
}
