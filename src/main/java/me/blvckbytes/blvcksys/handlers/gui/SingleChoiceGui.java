package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import net.minecraft.util.Tuple;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Allows the user to choose one of multiple paged choices, each represented
  by an itemstack with a corresponding bound object.
*/
@AutoConstruct
public class SingleChoiceGui extends AGui<SingleChoiceParam> {

  // Players which have chosen already
  private final Set<Player> haveChosen;

  public SingleChoiceGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_SINGLECHOICE_TITLE)
        .withVariable("type", i.getArg().type())
    ), plugin, cfg, textures);

    this.haveChosen = new HashSet<>();
  }

  @Override
  protected boolean closed(GuiInstance<SingleChoiceParam> inst) {
    if (!haveChosen.remove(inst.getViewer()))
      inst.getArg().closed().run();
    return false;
  }

  @Override
  protected boolean opening(Player viewer, GuiInstance<SingleChoiceParam> inst) {
    inst.addBorder(Material.BLACK_STAINED_GLASS_PANE);
    inst.addPagination(38, 40, 42);

    if (inst.getArg().backButton() != null)
      inst.addBack(36, e -> {
        haveChosen.add(viewer);
        inst.getArg().backButton().accept(e.getGui().getInv());
      });

    for (Tuple<Object, ItemStack> choice : inst.getArg().representitives()) {
      inst.addPagedItem(
        (i, s) -> choice.b(),
        e -> {
          haveChosen.add(viewer);
          if (e.getGui().getArg().selected().apply(choice.a(), inst.getInv()))
            e.getGui().close();
        },
        null
      );
    }

    return true;
  }
}
