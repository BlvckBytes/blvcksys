package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.commands.IEnderchestCommand;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  Listens for block interactions and may override default behavior.
*/
@AutoConstruct
public class InteractionListener implements Listener {

  private final IEnderchestCommand enderchest;

  public InteractionListener(
    @AutoInject IEnderchestCommand enderchest
  ) {
    this.enderchest = enderchest;
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
      return;

    if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.ENDER_CHEST)
      return;

    e.setCancelled(true);
    enderchest.openEnderchest(e.getPlayer());
  }
}
