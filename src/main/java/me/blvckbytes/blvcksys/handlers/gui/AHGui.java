package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IAHHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.AHStateModel;
import me.blvckbytes.blvcksys.util.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/09/2022

  Represents the main screen of the auction house, which takes care of displaying
  available auctions and offers multiple different ways to filter results.
*/
@AutoConstruct
public class AHGui extends AGui<Object> {

  private final IAHHandler ahHandler;
  private final SingleChoiceGui singleChoiceGui;
  private final ChatUtil chatUtil;
  private final AHProfileGui ahProfileGui;

  public AHGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject SingleChoiceGui singleChoiceGui,
    @AutoInject ChatUtil chatUtil,
    @AutoInject IAHHandler ahHandler,
    @AutoInject AHProfileGui ahProfileGui
  ) {
    super(6, "2-8,11-17,20-26,29-35,38-44", i -> (
      cfg.get(ConfigKey.GUI_AH)
    ), plugin, cfg, textures);

    this.ahHandler = ahHandler;
    this.singleChoiceGui = singleChoiceGui;
    this.chatUtil = chatUtil;
    this.ahProfileGui = ahProfileGui;

    // Refresh page contents of all AH GUI instances after an auction delta
    this.ahHandler.registerAuctionDeltaInterest(() -> {
      getActiveInstances().values().forEach(insts -> insts.forEach(GuiInstance::refreshPageContents));
    });
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
    ), e -> inst.switchTo(AnimationType.SLIDE_LEFT, ahProfileGui, null));

    // Spacer
    inst.addSpacer("50", Material.BLACK_STAINED_GLASS_PANE);

    // Paginator
    inst.addPagination(51, 52, 53);

    inst.setPageContents(() -> {
      // List all auctions based on the currently applied filters
      AHStateModel state = ahHandler.getState(inst.getViewer());
      return ahHandler.listAuctions(state.getCategory(), state.getSort(), state.getSearch())
        .stream().map(auction -> (
          new GuiItem(
            s -> (
              new ItemStackBuilder(auction.getItem(), auction.getItem().getAmount())
                .build()
            ),
            e -> {},
            10 // Redraw every 1s/2 to guarantee proper countdowns
          )
        ))
        .collect(Collectors.toList());
    });

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
      AHStateModel state = ahHandler.getState(inst.getViewer());

      // No search string defined
      if (state.getSearch() == null)
        search.append("/");

      // Wrap the search string
      else {
        int chPerLine = 35, remChPerLine = chPerLine;
        boolean isFirstLine = true;

        for (String word : state.getSearch().split(" ")) {
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
            state.getSearch() == null ?
              ConfigKey.GUI_AH_SEARCH_LORE_INACTIVE :
              ConfigKey.GUI_AH_SEARCH_LORE_ACTIVE
            )
            .withVariable("search", search)
        )
        .build();
    }, e -> {
      AHStateModel state = ahHandler.getState(inst.getViewer());

      // Reset an existing search string
      if (state.getSearch() != null) {
        state.setSearch(null);
        ahHandler.storeState(state);
        inst.redraw("*");
        return;
      }

      // Prompt for a new search string
      promptSearch(inst, search -> {
        state.setSearch(search);
        ahHandler.storeState(state);

        inst.refreshPageContents();
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
      AHStateModel state = ahHandler.getState(inst.getViewer());

      // Iterate all available sort types
      AuctionSort[] sorts = AuctionSort.values();
      for (int i = 0; i < sorts.length; i++) {
        selectionLines.append(
          // Append either an active or an inactive line,
          // based on the current selection
          cfg.get(
            state.getSort().equals(sorts[i]) ?
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
      AHStateModel state = ahHandler.getState(inst.getViewer());
      AuctionSort curr = state.getSort();
      AuctionSort[] sorts = AuctionSort.values();

      // Select the sort by it's index, corresponding to the key pressed
      int index;
      if (e.getClick() == ClickType.NUMBER_KEY)
        index = Math.min(sorts.length - 1, Math.max(0, e.getTargetSlot()));

      // Select the next index, wrapping around
      else
        index = (Arrays.binarySearch(sorts, curr) + 1) % sorts.length;

      state.setSort(sorts[index]);
      ahHandler.storeState(state);

      inst.refreshPageContents();
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
      AHStateModel state = ahHandler.getState(inst.getViewer());
      state.setCategory(category);
      ahHandler.storeState(state);

      inst.refreshPageContents();
      inst.redraw("*");
    });

    // Category status indicator next to the icon
    inst.fixedItem(slot + 1, () -> (
      new ItemStackBuilder(
        ahHandler.getState(inst.getViewer()).getCategory() == category ?
          Material.PURPLE_STAINED_GLASS_PANE :
          Material.BLACK_STAINED_GLASS_PANE
        )
        .withName(ConfigValue.immediate(" "))
        .build()
    ), null);
  }
}
