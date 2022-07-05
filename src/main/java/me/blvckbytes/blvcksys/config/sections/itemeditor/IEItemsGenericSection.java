package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.sections.ItemStackSection;
import me.blvckbytes.blvcksys.handlers.gui.IStdGuiItemProvider;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import me.blvckbytes.blvcksys.handlers.gui.StdGuiItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/01/2022

  Represents a section containing all shared GUI items.
*/
@Getter
public class IEItemsGenericSection extends AConfigSection implements IStdGuiItemProvider {

  private ItemStackBuilder background;
  private ItemStackBuilder nextPage;
  private ItemStackBuilder nextPageDisabled;
  private ItemStackBuilder previousPage;
  private ItemStackBuilder previousPageDisabled;
  private ItemStackBuilder currentPage;
  private ItemStackBuilder search;
  private ItemStackBuilder newChoice;
  private ItemStackBuilder submitChoices;
  private ItemStackBuilder submitChoicesDisabled;
  private ItemStackSection choiceSelected;
  private ItemStackBuilder searchPlaceholder;
  private ItemStackBuilder back;

  @Override
  public Object defaultFor(Class<?> type, String field) {
    if (type == ItemStackBuilder.class)
      return new ItemStackBuilder(Material.BARRIER).withName(ConfigValue.immediate("&cundefined"));
    return super.defaultFor(type, field);
  }

  @Override
  public ItemStack getItem(StdGuiItem item, @Nullable Map<String, String> variables) {
    return (switch (item) {
      case BACK -> back;
      case SEARCH -> search;
      case NEXT_PAGE -> nextPage;
      case NEXT_PAGE_DISABLED -> nextPageDisabled;
      case PREV_PAGE -> previousPage;
      case PREV_PAGE_DISABLED -> previousPageDisabled;
      case BACKGROUND -> background;
      case NEW_CHOICE -> newChoice;
      case PAGE_INDICATOR -> currentPage;
      case SUBMIT_CHOICES_ACTIVE -> submitChoices;
      case SUBMIT_CHOICES_DISABLED -> submitChoicesDisabled;
      case SEARCH_PLACEHOLDER -> searchPlaceholder;
    }).build(variables);
  }
}
