package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.ICrateHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.CrateItemModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import me.blvckbytes.blvcksys.util.ChatUtil;
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
  private final IPersistence pers;

  @AutoInjectLate
  private CrateContentGui crateContentGui;

  public CrateItemDetailGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject ICrateHandler crateHandler,
    @AutoInject ChatUtil chatUtil,
    @AutoInject IPersistence pers
  ) {
    super(6, "", i -> (
      cfg.get(ConfigKey.GUI_CRATE_DETAIL_NAME).
        withVariable("name", i.getArg().a().getName())
    ), plugin, cfg, textures);

    this.crateHandler = crateHandler;
    this.chatUtil = chatUtil;
    this.pers = pers;
  }

  @Override
  protected void prepare() {
    addFill(Material.BLACK_STAINED_GLASS_PANE);
    fixedItem("12,14,22", i -> new ItemStackBuilder(Material.PURPLE_STAINED_GLASS_PANE).build(), null);

    addBack(45, crateContentGui, i -> new Tuple<>(i.getArg().a(), true), AnimationType.SLIDE_RIGHT);

    // Selected item showcase
    fixedItem(13, i -> crateContentGui.appendDecoration(i.getArg().a(), i.getArg().b()), null);

    // Probability change
    fixedItem(29, i -> (
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
              pers.store(item);

              p.sendMessage(
                cfg.get(ConfigKey.COMMAND_CRATE_ITEM_UPDATED_PROBABILITY)
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
    fixedItem(40, i -> (
      new ItemStackBuilder(Material.BARRIER)
        .withName(cfg.get(ConfigKey.GUI_CRATE_DETAIL_DELETE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CRATE_DETAIL_DELETE_LORE))
        .build()
    ), i -> {
      boolean res = crateHandler.deleteItem(i.getGui().getArg().b());

      i.getGui().getViewer().sendMessage(
        cfg.get(res ? ConfigKey.COMMAND_CRATE_ITEM_DELETED : ConfigKey.COMMAND_CRATE_ITEM_DISAPPEARED)
          .withPrefix()
          .withVariable("item", crateContentGui.getItemName(i.getGui().getArg().b()))
          .withVariable("name", i.getGui().getArg().a().getName())
          .asScalar()
      );

      i.getGui().switchTo(i.getGui(), AnimationType.SLIDE_RIGHT, crateContentGui, new Tuple<>(i.getGui().getArg().a(), true));
    });

    // Invoke itemeditor on this item
    fixedItem(33, i -> (
      new ItemStackBuilder(Material.ARROW)
        .withName(cfg.get(ConfigKey.GUI_CRATE_DETAIL_EDIT_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CRATE_DETAIL_EDIT_LORE))
        .build()
    ), i -> {
      i.getGui().getViewer().sendMessage("§cThe editor is still to be implemented!");
    });
  }

  @Override
  protected void closed(GuiInstance<Tuple<CrateModel, CrateItemModel>> inst) {}

  @Override
  protected void opening(Player viewer, GuiInstance<Tuple<CrateModel, CrateItemModel>> inst) {}
}
