package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.sections.GuiLayoutSection;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
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
      ConfigValue.immediate(i.getArg().type())
    ), plugin, cfg, textures);

    this.haveChosen = new HashSet<>();
    this.searchGui = searchGui;
  }

  @Override
  protected boolean closed(GuiInstance<SingleChoiceParam> inst) {
    if (!haveChosen.remove(inst.getViewer())) {
      Consumer<GuiInstance<SingleChoiceParam>> closed = inst.getArg().closed();
      if (closed != null)
        closed.accept(inst);
    }
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<SingleChoiceParam> inst) {
    Player p = inst.getViewer();
    SingleChoiceParam arg = inst.getArg();
    IStdGuiItemProvider itemProvider = arg.itemProvider();
    GuiLayoutSection layout = arg.layout();

    if (!inst.applyLayoutParameters(layout, itemProvider))
      inst.addBorder(itemProvider);

    Map<String, String> slots = layout != null ? layout.getSlots() : new HashMap<>();

    inst.addPagination(
      slots.getOrDefault("prevPage", "38"),
      slots.getOrDefault("currentPage", "40"),
      slots.getOrDefault("nextPage", "42"),
      itemProvider
    );

    // Reopens this instance on the next tick when called
    Runnable reopen = () -> Bukkit.getScheduler().runTask(plugin, () -> inst.reopen(AnimationType.SLIDE_UP));

    // Search button
    inst.fixedItem(
      slots.getOrDefault("search", "44"),
      () -> itemProvider.getItem(StdGuiItem.SEARCH, null),
      e -> {
        // Create a carbon copy of the param and re-route callbacks
        SingleChoiceParam scp = new SingleChoiceParam(
          arg.type(), arg.representitives(), itemProvider, arg.layout(),
          arg.customFilter(), arg.selected(),

          // Re-open the choice if nothing was chosen or back was clicked
          (i) -> reopen.run(),
          arg.backButton() == null ? null :
          (i) -> reopen.run()
        );

        // Add to chosen just to not trigger any callbacks prematurely
        haveChosen.add(p);
        searchGui.show(p, scp, null);
      }, null
    );

    if (inst.getArg().backButton() != null) {
      inst.addBack(
        slots.getOrDefault("back", "36"),
        itemProvider, e -> {
          haveChosen.add(p);
          inst.getArg().backButton().accept(inst);
        }
      );
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
