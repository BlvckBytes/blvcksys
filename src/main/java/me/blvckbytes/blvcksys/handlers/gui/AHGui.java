package me.blvckbytes.blvcksys.handlers.gui;

import lombok.NonNull;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.util.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/09/2022

  Represents the main screen of the auction house, which takes care of displaying
  available auctions and offers multiple different ways to filter results.
*/
@AutoConstruct
public class AHGui extends AGui<Object> {

  // TODO: Persist this state
  private final Map<Player, AHGuiState> states;
  private final SingleChoiceGui singleChoiceGui;
  private final ChatUtil chatUtil;

  public AHGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject SingleChoiceGui singleChoiceGui,
    @AutoInject ChatUtil chatUtil
  ) {
    super(6, "2-8,11-17,20-26,29-35,38-44", i -> (
      cfg.get(ConfigKey.GUI_AH)
    ), plugin, cfg, textures);

    this.states = new HashMap<>();
    this.singleChoiceGui = singleChoiceGui;
    this.chatUtil = chatUtil;
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();

    // Left hand side category tabs
    itemCategoryTab(inst, 0, AuctionCategory.COMBAT);
    itemCategoryTab(inst, 9, AuctionCategory.ARMOR);
    itemCategoryTab(inst, 18, AuctionCategory.TOOLS);
    itemCategoryTab(inst, 27, AuctionCategory.BUILDING);
    itemCategoryTab(inst, 36, AuctionCategory.MISC);
    itemCategoryTab(inst, 45, AuctionCategory.ALL);

    // Auction sort type choice
    itemSortTypeCycler(inst, 47);

    // Free text search initiator
    itemFreeTextSearch(inst, 48);

    // Profile button
    inst.fixedItem(49, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(p.getName()))
        .withName(cfg.get(ConfigKey.GUI_AH_PROFILE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_AH_PROFILE_LORE))
        .build()
    ), e -> {});

    // Spacer
    inst.fixedItem(50, () -> new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).build(), null);

    // Paginator
    inst.addPagination(51, 52, 53);

    return true;
  }

  /**
   * Prompt the user for a new search query in the chat
   * @param inst GUI ref
   * @param search Search query callback
   */
  private void promptSearch(GuiInstance<Object> inst, Consumer<String> search) {
    new UserInputChain(inst, values -> {
      search.accept((String) values.get("search"));
    }, singleChoiceGui, chatUtil)
      .withPrompt(
        "search",
        values -> cfg.get(ConfigKey.GUI_AH_SEARCH_PROMPT).withPrefix(),
        ChatColor::stripColor, null, null
      )
      .start();
  }

  /**
   * Creates the free text search promting icon which opens a new chat prompt
   * or resets current values and displays the query in it's lore
   * @param inst GUI ref
   * @param slot Slot to put the type icon at
   */
  private void itemFreeTextSearch(GuiInstance<Object> inst, int slot) {
    inst.fixedItem(slot, () -> {
      StringBuilder search = new StringBuilder();
      AHGuiState state = getState(inst);

      // No search string defined
      if (state.search == null)
        search.append("/");

      // Wrap the search string
      else {
        int chPerLine = 35, remChPerLine = chPerLine;
        boolean isFirstLine = true;

        for (String word : state.search.split(" ")) {
          int wlen = word.length();

          if ((isFirstLine && wlen > remChPerLine / 2) || wlen > remChPerLine) {
            isFirstLine = false;
            search.append("\n").append(word).append(" ");
            remChPerLine = chPerLine;
            continue;
          }

          search.append(word).append(" ");
          remChPerLine -= wlen + 1;
        }
      }

      return new ItemStackBuilder(Material.OAK_SIGN)
        .withName(cfg.get(ConfigKey.GUI_AH_SEARCH_NAME))
        .withLore(
          cfg.get(
            state.search == null ?
              ConfigKey.GUI_AH_SEARCH_LORE_INACTIVE :
              ConfigKey.GUI_AH_SEARCH_LORE_ACTIVE
            )
            .withVariable("search", search)
        )
        .build();
    }, e -> {
      AHGuiState state = getState(inst);

      // Reset an existing search string
      if (state.search != null) {
        state.search = null;
        inst.redraw("*");
        return;
      }

      // Prompt for a new search string
      promptSearch(inst, search -> {
        state.search = search;
        inst.redraw("*");
      });
    });
  }

  /**
   * Creates the sort type cycler/selector which selects the next sort type
   * on click and allows for specific sort selection by the use of hotkeys
   * @param inst GUI ref
   * @param slot Slot to put the type icon at
   */
  private void itemSortTypeCycler(GuiInstance<Object> inst, int slot) {
    inst.fixedItem(slot, () -> {
      StringBuilder selectionLines = new StringBuilder();
      AHGuiState state = getState(inst);

      // Iterate all available sort types
      AuctionSort[] sorts = AuctionSort.values();
      for (int i = 0; i < sorts.length; i++) {
        selectionLines.append(
          // Append either an active or an inactive line,
          // based on the current selection
          cfg.get(
            state.sort.equals(sorts[i]) ?
              ConfigKey.GUI_AH_SORT_FORMAT_ACTIVE :
              ConfigKey.GUI_AH_SORT_FORMAT_INACTIVE
            )
            .withVariable("key", i + 1)
            .withVariable("name", sorts[i].getName(cfg))
            .asScalar()
        );

        // Add line breaks between entries
        if (i != sorts.length - 1)
          selectionLines.append('\n');
      }

      return new ItemStackBuilder(Material.HOPPER)
        .withName(cfg.get(ConfigKey.GUI_AH_SORT_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_AH_SORT_LORE)
            .withVariable("selection_lines", selectionLines)
        )
        .build();
    }, e -> {
      AHGuiState state = getState(inst);
      AuctionSort curr = state.sort;
      AuctionSort[] sorts = AuctionSort.values();

      // Select the sort by it's index, corresponding to the key pressed
      int index;
      if (e.getClick() == ClickType.NUMBER_KEY)
        index = Math.min(sorts.length - 1, Math.max(0, e.getTargetSlot()));

      // Select the next index, wrapping around
      else
        index = (Arrays.binarySearch(sorts, curr) + 1) % sorts.length;

      state.sort = sorts[index];
      inst.redraw("*");
    });
  }

  /**
   * Creates a category tab item which can select it's category and renders the
   * selected indicator next to it if it's category is currently active
   * @param inst GUI ref
   * @param slot Slot to put the category tab icon at
   * @param category Category that's represented
   */
  private void itemCategoryTab(GuiInstance<Object> inst, int slot, AuctionCategory category) {
    // Category icon
    inst.fixedItem(slot, () -> (
      new ItemStackBuilder(category.getMat())
        .withName(category.getName(cfg))
        .withLore(cfg.get(ConfigKey.GUI_AH_CAT_GENERIC_LORE))
        .hideAttributes()
        .build()
    ), e -> {
      getState(inst).cat = category;
      inst.redraw("*");
    });

    // Category status indicator next to the icon
    inst.fixedItem(slot + 1, () -> (
      new ItemStackBuilder(
        getState(inst).cat == category ?
          Material.PURPLE_STAINED_GLASS_PANE :
          Material.BLACK_STAINED_GLASS_PANE
        )
        .build()
    ), null);
  }

  /**
   * Get the GUI state bound to a GUI's viewer
   * @param inst GUI ref
   * @return Mutable GUI state
   */
  @NonNull
  private AHGuiState getState(GuiInstance<Object> inst) {
    Player p = inst.getViewer();

    if (states.containsKey(p))
      return states.get(p);

    AHGuiState state = AHGuiState.makeDefault();
    states.put(p, state);
    return state;
  }
}
