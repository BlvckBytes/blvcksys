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
  protected boolean opening(GuiInstance<Tuple<CrateModel, CrateItemModel>> inst) {
    Player p = inst.getViewer();
    CrateModel crate = inst.getArg().a();
    CrateItemModel item = inst.getArg().b();

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);
    inst.fixedItem("12,14,22", () -> new ItemStackBuilder(Material.PURPLE_STAINED_GLASS_PANE).build(), null);

    inst.addBack(45, crateContentGui, () -> new Tuple<>(inst.getArg().a(), true), AnimationType.SLIDE_RIGHT);

    // Selected item showcase
    inst.fixedItem(13, () -> crateContentGui.appendDecoration(inst.getArg().a(), inst.getArg().b()), null);

    // Probability change
    inst.fixedItem(29, () -> (
      new ItemStackBuilder(Material.GOLD_INGOT)
        .withName(cfg.get(ConfigKey.GUI_CRATE_DETAIL_PROBABILITY_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CRATE_DETAIL_PROBABILITY_LORE))
        .build()
    ), e -> {

      chatUtil.registerPrompt(
        p,
        cfg.get(ConfigKey.GUI_CRATE_DETAIL_PROBABILITY_PROMPT)
          .withPrefix()
          .asScalar(),

        // Probability entered
        probabilityStr -> {
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
          } catch (NumberFormatException ex) {
            p.sendMessage(
              cfg.get(ConfigKey.ERR_FLOATPARSE)
                .withPrefix()
                .withVariable("number", probabilityStr)
                .asScalar()
            );
            return;
          }

          inst.reopen(AnimationType.SLIDE_UP);
        },

        // Cancelled
        () -> inst.reopen(AnimationType.SLIDE_UP)
      );

      p.closeInventory();
    });

    // Delete button
    inst.fixedItem(40, () -> (
      new ItemStackBuilder(Material.BARRIER)
        .withName(cfg.get(ConfigKey.GUI_CRATE_DETAIL_DELETE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CRATE_DETAIL_DELETE_LORE))
        .build()
    ), i -> {
      // Prompt for deletion confirmation
      inst.switchTo(AnimationType.SLIDE_LEFT, confirmationGui, (confirmed, inv) -> {

        // Confirmed
        if (confirmed == TriResult.SUCC) {
          boolean res = crateHandler.deleteItem(item);

          p.sendMessage(
            cfg.get(res ? ConfigKey.COMMAND_CRATE_ITEM_DELETED : ConfigKey.COMMAND_CRATE_ITEM_DISAPPEARED)
              .withPrefix()
              .withVariable("item", crateContentGui.getItemName(item))
              .withVariable("name", crate.getName())
              .asScalar()
          );

          // Move back to the content gui
          crateContentGui.show(p, new Tuple<>(crate, true), AnimationType.SLIDE_RIGHT, inv);
          return false;
        }

        p.sendMessage(
          cfg.get(ConfigKey.GUI_CRATE_DETAIL_DELETE_CANCELLED)
            .withPrefix()
            .withVariable("item", crateContentGui.getItemName(item))
            .asScalar()
        );

        // Re-open the detail GUI if the confirmation wasn't closed
        if (confirmed != TriResult.EMPTY)
          this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, inv);

        return false;
      });
    });

    // Invoke itemeditor on this item
    inst.fixedItem(33, () -> (
      new ItemStackBuilder(Material.ARROW)
        .withName(cfg.get(ConfigKey.GUI_CRATE_DETAIL_EDIT_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CRATE_DETAIL_EDIT_LORE))
        .build()
    ), i -> {
      inst.switchTo(AnimationType.SLIDE_LEFT, itemEditorGui, new Triple<>(
        item.getItem(),

        // Store the item on edits
        edited -> {
          item.setItem(edited);
          crateHandler.updateItem(item);
        },

        // Re-open the detail GUI on completion
        editorInv -> {
          this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, editorInv);
        }
      ));
    });

    return true;
  }
}
