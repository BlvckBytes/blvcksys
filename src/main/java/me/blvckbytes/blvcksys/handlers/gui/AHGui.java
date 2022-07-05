package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IAHHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.AHAuctionModel;
import me.blvckbytes.blvcksys.persistence.models.AHBidModel;
import me.blvckbytes.blvcksys.persistence.models.AHStateModel;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.TimeUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
  private final TimeUtil timeUtil;
  private final AHBidGui ahBidGui;
  private final IStdGuiItemProvider stdGuiItemProvider;
  private final IPlayerTextureHandler textures;

  public AHGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject SingleChoiceGui singleChoiceGui,
    @AutoInject ChatUtil chatUtil,
    @AutoInject IAHHandler ahHandler,
    @AutoInject AHProfileGui ahProfileGui,
    @AutoInject TimeUtil timeUtil,
    @AutoInject AHBidGui ahBidGui,
    @AutoInject IStdGuiItemProvider stdGuiItemProvider
  ) {
    super(6, "2-8,11-17,20-26,29-35,38-44", i -> (
      cfg.get(ConfigKey.GUI_AH)
    ), plugin, cfg);

    this.textures = textures;
    this.ahHandler = ahHandler;
    this.singleChoiceGui = singleChoiceGui;
    this.chatUtil = chatUtil;
    this.ahProfileGui = ahProfileGui;
    this.timeUtil = timeUtil;
    this.ahBidGui = ahBidGui;
    this.stdGuiItemProvider = stdGuiItemProvider;

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
    itemFreeTextSearch(inst, "48");

    // Profile button
    inst.fixedItem("49", () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(p.getName()))
        .withName(cfg.get(ConfigKey.GUI_AH_PROFILE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_AH_PROFILE_LORE))
        .build()
    ), e -> inst.switchTo(AnimationType.SLIDE_LEFT, ahProfileGui, null), null);

    // Spacer
    inst.addSpacer("50", stdGuiItemProvider);

    // Paginator
    inst.addPagination("51", "52", "53", stdGuiItemProvider);

    inst.setPageContents(() -> {
      // List all auctions based on the currently applied filters
      AHStateModel state = ahHandler.getState(inst.getViewer());

      // List all auctions according to the state's filter
      List<AHAuctionModel> auctions = ahHandler.listPublicAuctions(
        state.getCategory(), state.getSort(), state.getSearch()
      );

      // No active auctions available
      if (auctions.size() == 0) {
        return List.of(
          new GuiItem(
            s -> (
              new ItemStackBuilder(Material.BARRIER)
                .withName(cfg.get(ConfigKey.GUI_AH_NONE_NAME))
                .withLore(cfg.get(ConfigKey.GUI_AH_NONE_LORE))
                .build()
            ), null, null
          )
        );
      }

      return auctions.stream().map(auction -> (
        new GuiItem(
          s -> {

            // An auction just ended, refresh contents
            if (!auction.isActive())
              inst.refreshPageContents();

            AHBidModel currBid = ahHandler.lastBid(auction, null).b();
            AHBidModel viewerBid = ahHandler.lastBid(auction, p).b();

            return buildDisplayItem(p, auction, currBid, viewerBid, null);
          },
          e -> inst.switchTo(AnimationType.SLIDE_LEFT, ahBidGui, auction),
          10 // Redraw every 1s/2 to guarantee proper synchronicity
        )
      ))
      .collect(Collectors.toList());
    });

    return true;
  }

  /**
   * Build the auction display item, which consists of the item being sold,
   * modified with an optional custom displayname and additional informative lore lines
   * @param viewer Viewing player
   * @param auction Auction to display
   * @param currBid Current last bid
   * @param additionalLore Additional lore to display, available variables are added internally
   * @return Item to display
   */
  public ItemStack buildDisplayItem(
    Player viewer,
    AHAuctionModel auction,
    @Nullable AHBidModel currBid,
    @Nullable AHBidModel viewerBid,
    @Nullable ConfigValue additionalLore
  ) {
    return new ItemStackBuilder(auction.getItem(), auction.getItem().getAmount())
      .withName(
        cfg.get(ConfigKey.GUI_AH_AUCTION_NAME)
          .withVariable(
            "name",
            getDisplayName(auction.getItem())
              .orElse(formatConstant(auction.getItem().getType().name()))
          )
      )
      .withLore(
        cfg.get(
          auction.getDurationSeconds() == null ?
            ConfigKey.GUI_AH_INSTANT_LORE :
            ConfigKey.GUI_AH_AUCTION_LORE_BASE
        )
        // Add bidding-specific information if the player is involved
        .joinWith(() ->
            cfg.get(ConfigKey.GUI_AH_AUCTION_LORE_BIDDING)
              .withVariable("viewer_bid", (viewerBid == null ? "/" : viewerBid.getAmount() + " Coins")),
          viewerBid != null
        )
        // Has been outbid, display actions
        .joinWith(() ->
            cfg.get(ConfigKey.GUI_AH_AUCTION_LORE_BIDDING_OUTBID),
          viewerBid != null && currBid != null && currBid.getCreator() != viewer
        )
        // Add any additional lore lines
        .joinWith(() -> additionalLore, additionalLore != null)
        .withVariable("is_highest", currBid == null ? "/" : statePlaceholderYN(viewer.equals(currBid.getCreator())))
        .withVariable("seller", auction.getCreator().getName())
        .withVariable("start_bid", (auction.getStartBid()) + " Coins")
        .withVariable("current_bid", (currBid == null ? "/" : currBid.getAmount() + " Coins"))
        .withVariable("current_bidder", currBid == null ? "/" : currBid.getCreator().getName())
        .withVariable("duration", getRemainingDuration(auction))
      )
      .build();
  }

  /**
   * Get the remaining formatted duration or fall back to the immediate string
   * @param auction Auction to read the duration from
   */
  private String getRemainingDuration(AHAuctionModel auction) {
    if (auction.getDurationSeconds() == null)
      return cfg.get(ConfigKey.GUI_CREATE_AH_DURATION_IMMEDIATE).asScalar();

    int duration = auction.getDurationSeconds();
    int elapsed = (int) (System.currentTimeMillis() - auction.getCreatedAt().getTime()) / 1000;
    int remaining = Math.max(0, duration - elapsed);

    return remaining > 0 ? timeUtil.formatDuration(remaining) : cfg.get(ConfigKey.GUI_CREATE_AH_DURATION_EXPIRED).asScalar();
  }

  /**
   * Get the custom displayname of an item
   * @param item Target item stack
   * @return Optional value, empty if there is no name set yet
   */
  private Optional<String> getDisplayName(ItemStack item) {
    ItemMeta meta = item.getItemMeta();

    if (meta == null)
      return Optional.empty();

    return meta.getDisplayName().isBlank() ? Optional.empty() : Optional.of(meta.getDisplayName());
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
   * @param slotExpr Slot to put the type icon at
   */
  private void itemFreeTextSearch(GuiInstance<Object> inst, String slotExpr) {
    inst.fixedItem(slotExpr, () -> {
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
    }, null);
  }

  /**
   * Creates the sort type cycler/selector which selects the next sort type
   * on click and allows for specific sort selection by the use of hotkeys
   * @param inst GUI ref
   * @param slot Slot to put the type icon at
   */
  private void itemSortTypeCycler(GuiInstance<Object> inst, int slot) {
    inst.fixedItem(String.valueOf(slot), () -> {
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
      int index = e.getHotbarKey()
        .map(key -> Math.min(sorts.length - 1, Math.max(0, key - 1)))
        // Just select the next index, wrapping around
        .orElseGet(() -> (Arrays.binarySearch(sorts, curr) + 1) % sorts.length);

      state.setSort(sorts[index]);
      ahHandler.storeState(state);

      inst.refreshPageContents();
      inst.redraw("*");
    }, null);
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
    inst.fixedItem(String.valueOf(slot), () -> (
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
    }, null);

    // Category status indicator next to the icon
    inst.fixedItem(String.valueOf(slot + 1), () -> (
      new ItemStackBuilder(
        ahHandler.getState(inst.getViewer()).getCategory() == category ?
          Material.PURPLE_STAINED_GLASS_PANE :
          Material.BLACK_STAINED_GLASS_PANE
        )
        .withName(ConfigValue.immediate(" "))
        .build()
    ), null, null);
  }
}
