package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Allows the user to choose one of multiple paged choices, each represented
  by an itemstack with a corresponding bound object.
*/
@AutoConstruct
public class MultipleChoiceGui extends AGui<MultipleChoiceParam> {

  private final Map<GuiInstance<MultipleChoiceParam>, List<Tuple<Object, ItemStack>>> playerChoices;
  private final Set<GuiInstance<MultipleChoiceParam>> tookAction;
  private final SingleChoiceGui singleChoiceGui;

  public MultipleChoiceGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject SingleChoiceGui singleChoiceGui
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      ConfigValue.immediate(i.getArg().type())
    ), plugin, cfg, textures);

    this.playerChoices = new HashMap<>();
    this.tookAction = new HashSet<>();
    this.singleChoiceGui = singleChoiceGui;
  }

  @Override
  protected boolean closed(GuiInstance<MultipleChoiceParam> inst) {
    Consumer<GuiInstance<MultipleChoiceParam>> closed = inst.getArg().closed();

    // Took no valid action
    if (!tookAction.remove(inst)) {
      playerChoices.remove(inst);

      // Fire close callback (cancelling)
      if (closed != null)
        closed.accept(inst);

      return false;
    }

    return false;
  }

  @Override
  protected boolean opening(GuiInstance<MultipleChoiceParam> inst) {
    MultipleChoiceParam arg = inst.getArg();
    IStdGuiParamProvider paramProvider = arg.paramProvider();

    inst.setAnimationsEnabled(paramProvider.areAnimationsEnabled());

    if (!playerChoices.containsKey(inst))
      playerChoices.put(inst, new ArrayList<>());

    List<Tuple<Object, ItemStack>> choices = playerChoices.get(inst);

    inst.addBorder(paramProvider);

    inst.addPagination("38", "40", "42", paramProvider);

    // Add another choice
    inst.fixedItem("26", () -> paramProvider.getItem(StdGuiItem.NEW_CHOICE,
      ConfigValue.makeEmpty()
        .withVariable("num_choices", choices.size())
        .withVariable("remaining_choices", arg.representitives().size() - choices.size())
        .exportVariables()
    ), e -> {
      // Allow to switch inventories temporary
      tookAction.add(inst);

      inst.switchTo(AnimationType.SLIDE_LEFT, singleChoiceGui, new SingleChoiceParam(
        arg.type(),

        // Filter out already selected representitives
        arg.representitives().stream()
          .filter(repr -> !choices.contains(repr))
          .toList(),

        paramProvider, arg.customFilter(),

        // Add the new selection to the list of choices
        (sel, selInst) -> {
          tookAction.remove(inst);

          inst.getArg().representitives().stream()
            .filter(r -> r.a().equals(sel))
            .findFirst().ifPresent(choice -> {
              // Do not allow for duplicate entries
              if (!choices.contains(choice))
                choices.add(choice);
            });

          inst.refreshPageContents();
          inst.reopen(AnimationType.SLIDE_RIGHT, selInst);
        },
        // Closed
        selInst -> {
          tookAction.remove(inst);
          Bukkit.getScheduler().runTask(plugin, () -> inst.reopen(AnimationType.SLIDE_UP));
        },
        // Back button
        arg.backButton() == null ? null :
        selInst -> inst.reopen(AnimationType.SLIDE_RIGHT, selInst)
      ));
    }, null);

    // Render the back button, if provided
    if (inst.getArg().backButton() != null) {
      inst.addBack("36", paramProvider, e -> {
        tookAction.add(inst);
        inst.getArg().backButton().accept(inst);
      });
    }

    // Submit the list of choices
    inst.fixedItem("44", () -> (
      paramProvider.getItem(
        choices.size() == 0 ? StdGuiItem.SUBMIT_CHOICES_DISABLED : StdGuiItem.SUBMIT_CHOICES_ACTIVE,
        ConfigValue.makeEmpty()
          .withVariable("num_choices", choices.size())
          .withVariable("remaining_choices", arg.representitives().size() - choices.size())
          .exportVariables()
      )
    ), e -> {
      // Nothing chosen, ignore interactions
      if (choices.size() == 0)
        return;

      tookAction.add(inst);

      // Call selection callback
      inst.getArg().selected().accept(choices.stream().map(Tuple::a).toList(), inst);
    }, null);

    // Draw the player's choices as pages
    inst.setPageContents(() -> (
      choices.stream()
        .map(choice -> new GuiItem(
          // Transform selected items, if a transformer has been provided
          s -> arg.selectionTransform() == null ? choice.b() : arg.selectionTransform().apply(choice.b()),
          // Remove choices by clicking on them
          e -> {
            choices.remove(choice);
            inst.refreshPageContents();

            // Redraw the submit button, in case the last choice
            // has been removed and it's rendered inactive again
            inst.redraw("44");
          },
          null
        ))
        .collect(Collectors.toList())
      ));

    return true;
  }
}
