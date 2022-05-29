package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import net.minecraft.util.Tuple;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Edit all possible properties of an itemstack while always
  having a preview available.
*/
@AutoConstruct
public class ItemEditorGui extends AGui<ItemStack> {

  private final SingleChoiceGui singleChoiceGui;

  public ItemEditorGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject SingleChoiceGui singleChoiceGui
  ) {
    super(5, "", i -> (
      cfg.get(ConfigKey.GUI_ITEMEDITOR_TITLE)
        .withVariable("item_type", i.getArg().getType().name())
    ), plugin, cfg, textures);

    this.singleChoiceGui = singleChoiceGui;
  }

  @Override
  protected boolean closed(GuiInstance<ItemStack> inst) {
    return false;
  }

  @Override
  protected boolean opening(Player viewer, GuiInstance<ItemStack> inst) {
    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);

    ItemStack item = inst.getArg();
    Player p = inst.getViewer();

    ///////////////////////////////////// Preview //////////////////////////////////////

    inst.fixedItem("12,14,22", i -> new ItemStackBuilder(Material.PURPLE_STAINED_GLASS_PANE).build(), null);
    inst.fixedItem(13, i -> item, null);

    ///////////////////////////////////// Material /////////////////////////////////////

    inst.fixedItem(28, i -> (
      new ItemStackBuilder(Material.CHEST)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_MATERIAL_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_MATERIAL_LORE))
        .build()
    ), e -> {
      // Representitive items for each material
      List<Tuple<Object, ItemStack>> representitives = Arrays.stream(Material.values())
        .filter(m -> !(
          m.isAir() ||
          m.isLegacy()
        ))
        .map(m -> (
          new Tuple<>((Object) m, (
            new ItemStackBuilder(m)
              .withName(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_MATERIAL_NAME)
                  .withVariable(
                    "hr_type",
                    WordUtils.capitalizeFully(m.name().replace("_", " "))
                  )
              )
              .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_MATERIAL_LORE))
              .build()
          ))
        )
      ).toList();

      // Invoke a new single choice gui for available materials
      inst.switchTo(AnimationType.SLIDE_LEFT, singleChoiceGui, new SingleChoiceParam(
        cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_MATERIAL_TITLE).asScalar(), representitives,

        // Material selected
        (m, inv) -> {
          Material mat = (Material) m;

          item.setType(mat);
          this.show(p, item, AnimationType.SLIDE_RIGHT, inv);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_MATERIAL_CHANGED)
              .withPrefix()
              .withVariable("material", mat.name())
              .asScalar()
          );
          return false;
        },

        // Inventory closed, re-open the editor
        () -> Bukkit.getScheduler().runTaskLater(plugin, () -> this.show(p, item, AnimationType.SLIDE_UP), 1),

        // Back button
        inv -> this.show(viewer, item, AnimationType.SLIDE_RIGHT, inv)
      ));
    });

    return true;
  }
}
