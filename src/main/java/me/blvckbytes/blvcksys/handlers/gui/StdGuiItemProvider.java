package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.util.SymbolicHead;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/03/2022

  Uses as a standard GUI items provider for all internal playgrounds.
*/
@AutoConstruct
public class StdGuiItemProvider implements IStdGuiItemProvider {

  private final IConfig cfg;
  private final IPlayerTextureHandler textureHandler;

  public StdGuiItemProvider(
    @AutoInject IConfig cfg,
    @AutoInject IPlayerTextureHandler textureHandler
  ) {
    this.cfg = cfg;
    this.textureHandler = textureHandler;
  }

  @Override
  public ItemStack getItem(StdGuiItem item, @Nullable Map<String, String> variables) {
    return switch (item) {
      case BACKGROUND -> new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).withName(ConfigValue.immediate(" ")).build();

      case SEARCH -> new ItemStackBuilder(Material.NAME_TAG)
          .withName(cfg.get(ConfigKey.GUI_SINGLECHOICE_SEARCH_NAME))
          .withLore(cfg.get(ConfigKey.GUI_SINGLECHOICE_SEARCH_LORE))
          .build();

      case SEARCH_PLACEHOLDER -> new ItemStackBuilder(Material.PURPLE_TERRACOTTA)
          .withName(ConfigValue.immediate(" "))
          .withLore(cfg.get(ConfigKey.GUI_ANVILSEARCH_ITEM_LORE))
          .build();

      case PREV_PAGE_DISABLED, PREV_PAGE -> new ItemStackBuilder(textureHandler.getProfileOrDefault(SymbolicHead.ARROW_LEFT.getOwner()))
          .withName(cfg.get(ConfigKey.GUI_GENERICS_PAGING_PREV_NAME))
          .withLore(cfg.get(ConfigKey.GUI_GENERICS_PAGING_PREV_LORE))
          .build();

      case PAGE_INDICATOR -> new ItemStackBuilder(Material.PAPER)
          .withName(cfg.get(ConfigKey.GUI_GENERICS_PAGING_INDICATOR_NAME).withVariables(variables))
          .withLore(cfg.get(ConfigKey.GUI_GENERICS_PAGING_INDICATOR_LORE).withVariables(variables))
          .build();

      case NEXT_PAGE, NEXT_PAGE_DISABLED -> new ItemStackBuilder(textureHandler.getProfileOrDefault(SymbolicHead.ARROW_RIGHT.getOwner()))
          .withName(cfg.get(ConfigKey.GUI_GENERICS_PAGING_NEXT_NAME))
          .withLore(cfg.get(ConfigKey.GUI_GENERICS_PAGING_NEXT_LORE))
          .build();

      case BACK -> new ItemStackBuilder(textureHandler.getProfileOrDefault(SymbolicHead.ARROW_LEFT.getOwner()))
          .withName(cfg.get(ConfigKey.GUI_GENERICS_NAV_BACK_NAME))
          .withLore(cfg.get(ConfigKey.GUI_GENERICS_NAV_BACK_LORE))
          .build();

      case NEW_CHOICE -> new ItemStackBuilder(textureHandler.getProfileOrDefault(SymbolicHead.GREEN_PLUS.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_MULTIPLECHOICE_ADD_NAME))
        .withLore(cfg.get(ConfigKey.GUI_MULTIPLECHOICE_ADD_LORE))
        .build();

      case SUBMIT_CHOICES_ACTIVE -> new ItemStackBuilder(textureHandler.getProfileOrDefault(SymbolicHead.ARROW_RIGHT.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_MULTIPLECHOICE_SUBMIT_NAME))
        .withLore(cfg.get(ConfigKey.GUI_MULTIPLECHOICE_SUBMIT_LORE))
        .build();

      case SUBMIT_CHOICES_DISABLED -> new ItemStackBuilder(textureHandler.getProfileOrDefault(SymbolicHead.ARROW_RIGHT.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_MULTIPLECHOICE_SUBMIT_DISABLED_NAME))
        .withLore(cfg.get(ConfigKey.GUI_MULTIPLECHOICE_SUBMIT_DISABLED_LORE))
        .build();

    };
  }
}
