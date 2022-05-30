package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.ICrateHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.persistence.models.CrateItemModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.Triple;
import net.minecraft.util.Tuple;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Allows to change a specific crate item.
*/
@AutoConstruct
public class CrateItemDetailGui extends AGui<Tuple<CrateModel, CrateItemModel>> {

  private final ICrateHandler crateHandler;
  private final ChatUtil chatUtil;
  private final ConfirmationGui confirmationGui;
  private final ItemEditorGui itemEditorGui;

  @AutoInjectLate
  private CrateContentGui crateContentGui;

  public CrateItemDetailGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject ICrateHandler crateHandler,
    @AutoInject ChatUtil chatUtil,
    @AutoInject ConfirmationGui confirmationGui,
    @AutoInject ItemEditorGui itemEditorGui
  ) {
    super(6, "", i -> (
      cfg.get(ConfigKey.GUI_CRATE_DETAIL_NAME).
        withVariable("name", i.getArg().a().getName())
    ), plugin, cfg, textures);

    this.crateHandler = crateHandler;
    this.chatUtil = chatUtil;
    this.confirmationGui = confirmationGui;
    this.itemEditorGui = itemEditorGui;
  }

  @Override
  protected boolean closed(GuiInstance<Tuple<CrateModel, CrateItemModel>> inst) {
    return false;
  }

  @Override
  protected boolean opening(Player viewer, GuiInstance<Tuple<CrateModel, CrateItemModel>> inst) {
    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);
    inst.fixedItem("12,14,22", i -> new ItemStackBuilder(Material.PURPLE_STAINED_GLASS_PANE).build(), null);

    inst.addBack(45, crateContentGui, i -> new Tuple<>(i.getArg().a(), true), AnimationType.SLIDE_RIGHT);

    // Selected item showcase
    inst.fixedItem(13, i -> crateContentGui.appendDecoration(i.getArg().a(), i.getArg().b()), null);

    // Probability change
    inst.fixedItem(29, i -> (
      new ItemStackBuilder(Material.GOLD_INGOT)
        .withName(cfg.get(ConfigKey.GUI_CRATE_DETAIL_PROBABILITY_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CRATE_DETAIL_PROBABILITY_LORE))
        .build()
    ), i -> {

      chatUtil.registerPrompt(
        i.getGui().getViewer(),
        cfg.get(ConfigKey.GUI_CRATE_DETAIL_PROBABILITY_PROMPT)
          .withPrefix()
          .asScalar(),

        // Probability entered
        probabilityStr -> {
          CrateItemModel item = i.getGui().getArg().b();
          Player p = i.getGui().getViewer();

          try {
            // Parse the probability
            float probability = Float.parseFloat(probabilityStr);
            if (probability <= 0 || probability >= 100) {
              p.sendMessage(
                cfg.get(ConfigKey.COMMAND_CRATE_ITEM_INVALID_PROBABILITY)
                  .withPrefix()
                  .asScalar()
              );
            }

            // Set and update the value before re-opening the gui
            else {
              item.setProbability(probability);
              boolean res = crateHandler.updateItem(item);

              p.sendMessage(
                cfg.get(res ? ConfigKey.COMMAND_CRATE_ITEM_UPDATED_PROBABILITY : ConfigKey.COMMAND_CRATE_ITEM_DISAPPEARED)
                  .withPrefix()
                  .withVariable("item", crateContentGui.getItemName(item))
                  .withVariable("probability", Math.round(probability * 100F) / 100F)
                  .asScalar()
              );
            }
          } catch (NumberFormatException e) {
            i.getGui().getViewer().sendMessage(
              cfg.get(ConfigKey.ERR_FLOATPARSE)
                .withPrefix()
                .withVariable("number", probabilityStr)
                .asScalar()
            );
            return;
          }

          i.getGui().reopen(AnimationType.SLIDE_UP);
        },

        // Cancelled
        () -> i.getGui().reopen(AnimationType.SLIDE_UP)
      );

      i.getGui().getViewer().closeInventory();
    });

    // Delete button
    inst.fixedItem(40, i -> (
      new ItemStackBuilder(Material.BARRIER)
        .withName(cfg.get(ConfigKey.GUI_CRATE_DETAIL_DELETE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CRATE_DETAIL_DELETE_LORE))
        .build()
    ), i -> {
      // Prompt for deletion confirmation
      i.getGui().switchTo(AnimationType.SLIDE_LEFT, confirmationGui, (confirmed, inv) -> {

        // Confirmed
        if (confirmed == TriResult.SUCC) {
          boolean res = crateHandler.deleteItem(i.getGui().getArg().b());

          i.getGui().getViewer().sendMessage(
            cfg.get(res ? ConfigKey.COMMAND_CRATE_ITEM_DELETED : ConfigKey.COMMAND_CRATE_ITEM_DISAPPEARED)
              .withPrefix()
              .withVariable("item", crateContentGui.getItemName(i.getGui().getArg().b()))
              .withVariable("name", i.getGui().getArg().a().getName())
              .asScalar()
          );

          // Move back to the content gui
          crateContentGui.show(i.getGui().getViewer(), new Tuple<>(i.getGui().getArg().a(), true), AnimationType.SLIDE_RIGHT, inv);
          return false;
        }

        i.getGui().getViewer().sendMessage(
          cfg.get(ConfigKey.GUI_CRATE_DETAIL_DELETE_CANCELLED)
            .withPrefix()
            .withVariable("item", crateContentGui.getItemName(i.getGui().getArg().b()))
            .asScalar()
        );

        // Re-open the detail GUI if the confirmation wasn't closed
        if (confirmed != TriResult.EMPTY)
          this.show(i.getGui().getViewer(), i.getGui().getArg(), AnimationType.SLIDE_RIGHT, inv);

        return false;
      });
    });

    // Invoke itemeditor on this item
    inst.fixedItem(33, i -> (
      new ItemStackBuilder(Material.ARROW)
        .withName(cfg.get(ConfigKey.GUI_CRATE_DETAIL_EDIT_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CRATE_DETAIL_EDIT_LORE))
        .build()
    ), i -> {
      CrateItemModel model = i.getGui().getArg().b();

      i.getGui().switchTo(AnimationType.SLIDE_LEFT, itemEditorGui, new Triple<>(
        model.getItem(),

        // Store the item on edits
        edited -> {
          model.setItem(edited);
          crateHandler.updateItem(model);
        },

        // Re-open the detail GUI on completion
        editorInv -> {
          this.show(viewer, i.getGui().getArg(), AnimationType.SLIDE_RIGHT, editorInv);
        }
      ));
    });

    return true;
  }
}
