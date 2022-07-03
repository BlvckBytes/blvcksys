package me.blvckbytes.blvcksys.config.sections.itemeditor;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.handlers.gui.IStdGuiParamProvider;
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
public class IEItemsGenericSection extends AConfigSection implements IStdGuiParamProvider {

  private ItemStackBuilder background;
  private ItemStackBuilder nextPage;
  private ItemStackBuilder previousPage;
  private ItemStackBuilder currentPage;
  private ItemStackBuilder search;
  private ItemStackBuilder newChoice;
  private ItemStackBuilder submitChoices;
  private ItemStackBuilder choiceSelected;
  private ItemStackBuilder searchPlaceholder;
  private ItemStackBuilder back;
  private boolean animate;

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
      case PREV_PAGE -> previousPage;
      case BACKGROUND -> background;
      case NEW_CHOICE -> newChoice;
      case PAGE_INDICATOR -> currentPage;
      case SUBMIT_CHOICES -> submitChoices;
      case SEARCH_PLACEHOLDER -> searchPlaceholder;
    }).build(variables);
  }

  @Override
  public boolean areAnimationsEnabled() {
    return animate;
  }
}
