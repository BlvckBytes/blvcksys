package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.gui.AnimationType;
import me.blvckbytes.blvcksys.handlers.gui.ItemEditorGui;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Invokes a new itemeditor session on the item currently held in the main hand.
*/
@AutoConstruct
public class ItemEditorCommand extends APlayerCommand {

  private final ItemEditorGui itemEditorGui;

  public ItemEditorCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ItemEditorGui itemEditorGui
  ) {
    super(
      plugin, logger, cfg, refl,
      "itemeditor,ie",
      "Edit the item held in your hand",
      PlayerPermission.COMMAND_ITEMEDITOR
    );

    this.itemEditorGui = itemEditorGui;
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    ItemStack stack = p.getInventory().getItemInMainHand();

    // Has to have something in their hand
    if (stack.getType() == Material.AIR) {
      p.sendMessage(
        cfg.get(ConfigKey.ITEMEDITOR_NO_ITEM)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    itemEditorGui.show(p, stack, AnimationType.SLIDE_UP);
  }
}
