package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.KitModel;
import org.apache.commons.lang.WordUtils;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Displays the contents of a kit.
*/
@AutoConstruct
public class KitContentGui extends AGui<KitModel> {

  @AutoInjectLate
  private KitsGui kitsGui;

  private final IStdGuiItemProvider stdGuiItemProvider;

  public KitContentGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IStdGuiItemProvider stdGuiItemProvider
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_KIT_CONTENT_TITLE)
        .withVariable("name", i.getArg().getName())
    ), plugin, cfg, textures);

    this.stdGuiItemProvider = stdGuiItemProvider;
  }

  @Override
  protected boolean closed(GuiInstance<KitModel> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<KitModel> inst) {
    inst.addBorder(stdGuiItemProvider);

    inst.addBack("36", stdGuiItemProvider, kitsGui, null, AnimationType.SLIDE_RIGHT);

    inst.setPageContents(() -> (
      Arrays.stream(inst.getArg().getItems().getContents())
        .map(is -> {
          if (is == null)
            return null;

          ItemMeta meta = is.getItemMeta();
          String name = meta == null ? null : meta.getDisplayName();

          return new GuiItem(
            s -> (
              new ItemStackBuilder(is, is.getAmount())
              // Keep either the name from ItemMeta or set a human readable type fallback
              .withName(
                (name != null && !name.isBlank()) ? ConfigValue.immediate(name) :
                  cfg.get(ConfigKey.GUI_KIT_CONTENT_CONTENT_NAME)
                    .withVariable(
                      "hr_type",
                      WordUtils.capitalizeFully(is.getType().name().replace("_", " "))
                    )
              )
              .withLore(
                cfg.get(ConfigKey.GUI_KIT_CONTENT_CONTENT_LORE)
                  .withVariable("name", inst.getArg().getName())
              )
              .build()
            ), null, null
          );
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList())
    ));

    return true;
  }
}
