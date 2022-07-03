package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.gui.AnvilSearchGui;
import me.blvckbytes.blvcksys.handlers.gui.IStdGuiItemsProvider;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import me.blvckbytes.blvcksys.handlers.gui.SingleChoiceParam;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/04/2022

  Search through all existing heads within the local database.
 */
@AutoConstruct
public class HeadsCommand extends APlayerCommand {

  private final IPlayerTextureHandler textureHandler;
  private final AnvilSearchGui anvilSearchGui;
  private final IStdGuiItemsProvider stdGuiItemsProvider;

  public HeadsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPlayerTextureHandler textureHandler,
    @AutoInject AnvilSearchGui anvilSearchGui,
    @AutoInject IStdGuiItemsProvider stdGuiItemsProvider
    ) {
    super(
      plugin, logger, cfg, refl,
      "heads",
      "Open the head-database",
      PlayerPermission.COMMAND_HEADS.toString()
    );

    this.textureHandler = textureHandler;
    this.anvilSearchGui = anvilSearchGui;
    this.stdGuiItemsProvider = stdGuiItemsProvider;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    SingleChoiceParam scp = new SingleChoiceParam(
      cfg.get(ConfigKey.GUI_HEADS_SEARCH_NAME).asScalar(),
      new ArrayList<>(), stdGuiItemsProvider,

      search -> (
        // Only return as many results at max as will fit into the search GUI
        textureHandler.searchByName(search, 4 * 9)
          .stream()

          // Map each model to a tuple where the key is the vanilla head and the
          .map(model -> new Tuple<>(
            (Object) new ItemStackBuilder(model.toProfile()).build(),
            new ItemStackBuilder(model.toProfile())
              .withName(
                cfg.get(ConfigKey.GUI_HEADS_HEAD_NAME)
                  .withVariable("owner", model.getName())
              )
              .withLore(cfg.get(ConfigKey.GUI_HEADS_HEAD_LORE))
              .build()
          ))
          .toList()
      ),

      (selection, inst) -> {
        p.getInventory().addItem((ItemStack) selection);
        inst.close();
      },

      null, null
    );

    anvilSearchGui.show(p, scp, null);
  }
}
