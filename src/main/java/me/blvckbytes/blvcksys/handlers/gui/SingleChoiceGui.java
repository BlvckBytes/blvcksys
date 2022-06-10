package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

  private final AnvilSearchGui searchGui;

  public SingleChoiceGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject AnvilSearchGui searchGui
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_SINGLECHOICE_TITLE)
        .withVariable("type", i.getArg().type())
    ), plugin, cfg, textures);

    this.haveChosen = new HashSet<>();
    this.searchGui = searchGui;
  }

  @Override
  protected boolean closed(GuiInstance<SingleChoiceParam> inst) {
    if (!haveChosen.remove(inst.getViewer()))
      inst.getArg().closed().accept(inst);
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<SingleChoiceParam> inst) {
    Player p = inst.getViewer();

    inst.addBorder(Material.BLACK_STAINED_GLASS_PANE);
    inst.addPagination(38, 40, 42);

    // Reopens this instance on the next tick when called
    Runnable reopen = () -> Bukkit.getScheduler().runTask(plugin, () -> inst.reopen(AnimationType.SLIDE_UP));

    // Search button
    inst.fixedItem(44, () -> (
      new ItemStackBuilder(Material.NAME_TAG)
        .withName(cfg.get(ConfigKey.GUI_SINGLECHOICE_SEARCH_NAME))
        .withLore(cfg.get(ConfigKey.GUI_SINGLECHOICE_SEARCH_LORE))
        .build()
    ), e -> {
      // Create a carbon copy of the param and re-route callbacks
      SingleChoiceParam scp = new SingleChoiceParam(
        inst.getArg().type(), inst.getArg().representitives(),
        inst.getArg().customFilter(), inst.getArg().selected(),

        // Re-open the choice if nothing was chosen or back was clicked
        (i) -> reopen.run(), (i) -> reopen.run()
      );

      // Add to chosen just to not trigger any callbacks prematurely
      haveChosen.add(p);
      searchGui.show(p, scp, null);
    });

    if (inst.getArg().backButton() != null) {
      inst.addBack(36, e -> {
        haveChosen.add(p);
        inst.getArg().backButton().accept(inst);
      });
    }

    inst.setPageContents(() -> (
      inst.getArg().representitives().stream()
        .map(choice -> new GuiItem(
          s -> choice.b(),
          e -> {
            haveChosen.add(p);
            inst.getArg().selected().accept(choice.a(), inst);
          },
          null
        ))
        .collect(Collectors.toList())
      ));

    return true;
  }
}
