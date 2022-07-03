package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.util.SymbolicHead;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
    this.singleChoiceGui = singleChoiceGui;
  }

  @Override
  protected boolean closed(GuiInstance<MultipleChoiceParam> inst) {
    List<Tuple<Object, ItemStack>> choices = playerChoices.get(inst);
    Consumer<GuiInstance<MultipleChoiceParam>> closed = inst.getArg().closed();

    // Not registered, ignore
    if (choices == null)
      return false;

    // Made no choices, fire close callback (cancelling)
    if (choices.size() == 0 && closed != null) {
      playerChoices.remove(inst);
      closed.accept(inst);
      return false;
    }

    return false;
  }

  @Override
  protected boolean opening(GuiInstance<MultipleChoiceParam> inst) {
    if (!playerChoices.containsKey(inst))
      playerChoices.put(inst, new ArrayList<>());

    List<Tuple<Object, ItemStack>> choices = playerChoices.get(inst);

    inst.addBorder(new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).build());
    inst.addPagination(38, 40, 42);

    // Add another choice
    inst.fixedItem(26, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.GREEN_PLUS.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_MULTIPLECHOICE_ADD_NAME))
        .withLore(cfg.get(ConfigKey.GUI_MULTIPLECHOICE_ADD_LORE))
        .build()
    ), e -> {
      // Allow to switch inventories temporary
      if (choices.size() == 0)
        choices.add(null);

      inst.switchTo(AnimationType.SLIDE_LEFT, singleChoiceGui, new SingleChoiceParam(
        inst.getArg().type(), inst.getArg().representitives(), inst.getArg().customFilter(),
        // Add the new selection to the list of choices
        (sel, selInst) -> {
          choices.remove(null);

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
          choices.remove(null);
          Bukkit.getScheduler().runTask(plugin, () -> inst.reopen(AnimationType.SLIDE_UP));
        },
        // Back button
        selInst -> {
          choices.remove(null);
          inst.reopen(AnimationType.SLIDE_RIGHT, selInst);
        }
      ));
    }, null);

    // Render the back button, if provided
    if (inst.getArg().backButton() != null) {
      inst.addBack(36, e -> {
        choices.add(null);
        inst.getArg().backButton().accept(inst);
      });
    }

    // Submit the list of choices
    inst.fixedItem(44, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.ARROW_RIGHT.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_MULTIPLECHOICE_SUBMIT_NAME))
        .withLore(cfg.get(ConfigKey.GUI_MULTIPLECHOICE_SUBMIT_LORE))
        .build()
    ), e -> {
      // Nothing chosen, relay to the close callback (cancelling)
      if (choices.size() == 0) {
        inst.close();
        return;
      }

      // Call selection callback
      inst.getArg().selected().accept(choices.stream().map(Tuple::a).toList(), inst);
    }, null);

    // Draw the player's choices as pages
    inst.setPageContents(() -> (
      choices.stream()
        .map(choice -> new GuiItem(
          // Override the item's lore
          s -> (
            new ItemStackBuilder(choice.b(), choice.b().getAmount())
              .setLore(cfg.get(ConfigKey.GUI_MULTIPLECHOICE_SELECTED_LORE))
              .build()
          ),
          // Remove choices by clicking on them
          e -> {
            choices.remove(choice);
            inst.refreshPageContents();
          },
          null
        ))
        .collect(Collectors.toList())
      ));

    return true;
  }
}
