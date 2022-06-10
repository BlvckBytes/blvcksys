package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.handlers.gui.AuctionCategory;
import me.blvckbytes.blvcksys.handlers.gui.AuctionSort;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.AHAuctionModel;
import me.blvckbytes.blvcksys.persistence.models.AHBidModel;
import me.blvckbytes.blvcksys.persistence.models.AHStateModel;
import me.blvckbytes.blvcksys.persistence.query.*;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/10/2022

  Handles storing the main AH GUI's state as well as all auctions, bids,
  returned items and the like.
 */
@AutoConstruct
public class AHHandler implements IAHHandler, Listener, IAutoConstructed {

  // Default maximum number of auctions a player may have
  private static final int DEFAULT_MAX_AUCTIONS = 2;

  private final IPersistence pers;
  private final Map<Player, AHStateModel> stateCache;
  private final Map<AHAuctionModel, List<AHBidModel>> auctionCache;
  private final List<Runnable> auctionDeltaInterests;

  public AHHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
    this.auctionDeltaInterests = new ArrayList<>();
    this.stateCache = new HashMap<>();
    this.auctionCache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public AHStateModel getState(OfflinePlayer p) {
    // Respond from cache
    if (p instanceof Player onP && stateCache.containsKey(onP))
      return stateCache.get(onP);

    // Try to fetch from DB
    return pers.findFirst(buildStateQuery(p))
      // Not in DB yet
      .orElseGet(() -> {
        // Create, store and cache a new default instance
        AHStateModel def = AHStateModel.makeDefault(p);
        storeState(def);

        // Don't cache offline players
        if (p instanceof Player onP)
          stateCache.put(onP, def);

        return def;
      });
  }

  @Override
  public void storeState(AHStateModel state) {
    pers.store(state);
  }

  @Override
  public int countActiveAuctions(Player creator) {
    return pers.count(buildAuctionQuery(creator, true));
  }

  @Override
  public int getMaxAuctions(Player creator) {
    return PlayerPermission.AH_PARALLEL.getSuffixNumber(creator, true).orElse(DEFAULT_MAX_AUCTIONS);
  }

  @Override
  public boolean createAuction(Player creator, ItemStack item, int startBid, int durationSeconds, AuctionCategory category) {
    // Reached the maximum number of parallel auctions
    int maxAuctions = getMaxAuctions(creator);
    if (maxAuctions <= countActiveAuctions(creator))
      return false;

    AHAuctionModel auction = new AHAuctionModel(creator, item, durationSeconds, startBid, category, null, false);
    pers.store(auction);
    auctionCache.put(auction, new ArrayList<>());
    auctionDeltaInterests.forEach(Runnable::run);
    return true;
  }

  @Override
  public void deleteAuction(AHAuctionModel auction) {
    pers.delete(auction);
    auctionCache.remove(auction);
    auctionDeltaInterests.forEach(Runnable::run);
  }

  @Override
  public TriResult cancelAuction(OfflinePlayer executor, AHAuctionModel auction) {
    AHAuctionModel target = getAuctionById(auction.getId()).orElse(null);

    // Auction doesn't exist anymore
    if (target == null)
      return TriResult.EMPTY;

    // Already expired
    if (!target.isActive())
      return TriResult.EMPTY;

    // Already cancelled
    if (target.getCanceller() != null)
      return TriResult.ERR;

    // Cancel by setting the nullable canceller and storing afterwards
    target.setCanceller(executor);
    pers.store(target);
    auctionDeltaInterests.forEach(Runnable::run);
    return TriResult.SUCC;
  }

  @Override
  public TriResult createBid(OfflinePlayer executor, AHAuctionModel auction, int amount) {
    AHAuctionModel target = getAuctionById(auction.getId()).orElse(null);
    List<AHBidModel> bids = auctionCache.get(target);

    // Auction doesn't exist anymore
    if (target == null || bids == null)
      return TriResult.EMPTY;

    // Already expired or cancelled
    if (!target.isActive() || target.getCanceller() != null)
      return TriResult.EMPTY;

    // Has been outbid already
    if (bids.size() != 0 && bids.get(bids.size() - 1).getAmount() >= amount)
      return TriResult.ERR;

    // Create the new bid
    AHBidModel bid = new AHBidModel(executor, target.getId(), amount);
    pers.store(bid);
    bids.add(bid);
    auctionDeltaInterests.forEach(Runnable::run);

    return TriResult.SUCC;
  }

  @Override
  public List<AHAuctionModel> listAuctions(AuctionCategory category, AuctionSort sort, @Nullable String searchQuery) {
    return auctionCache.keySet().stream()
      .filter(auction -> (
        // Category matches or is a wildcard
        (category == AuctionCategory.ALL || auction.getCategory().equals(category)) &&
        // Search matches or is a wildcard
        (searchQuery == null || matchesSearch(auction.getItem(), searchQuery))
      ))
      .collect(Collectors.toList());
  }

  @Override
  public Optional<List<AHBidModel>> listBids(UUID auctionId) {
    return auctionCache.keySet().stream()
      .filter(auction -> auction.getId().equals(auctionId))
      .findFirst()
      .map(auctionCache::get);
  }

  @Override
  public void registerAuctionDeltaInterest(Runnable delta) {
    auctionDeltaInterests.add(delta);
  }

  @Override
  public void cleanup() {
    stateCache.clear();
    auctionCache.clear();
  }

  @Override
  public void initialize() {
    // Load all available auctions and bids from persistence
    List<AHAuctionModel> auctions = pers.list(AHAuctionModel.class);
    List<AHBidModel> bids = pers.list(AHBidModel.class);

    // Match auctions and their bids into cache
    for (AHAuctionModel auctionModel : auctions) {

      // Sort bids by amount descending, so bids(len-1) is always the highest bid
      List<AHBidModel> auctionBids = bids.stream()
        .filter(bid -> bid.getAuctionId().equals(auctionModel.getId()))
        .sorted((a, b) -> b.getAmount() - a.getAmount())
        .collect(Collectors.toList());

      // Remove taken bids and write to cache
      bids.removeAll(auctionBids);
      auctionCache.put(auctionModel, auctionBids);
    }
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    stateCache.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Get an auction by it's ID
   * @param id ID of the target auction
   */
  private Optional<AHAuctionModel> getAuctionById(UUID id) {
    return auctionCache.keySet().stream()
      .filter(auction -> auction.getId().equals(id))
      .findFirst();
  }

  /**
   * Checks if the given item matches the search query in any way
   * @param item Item to check
   * @param searchQuery Search query
   */
  private boolean matchesSearch(ItemStack item, String searchQuery) {
    String query = ChatColor.stripColor(searchQuery.toLowerCase());

    // Material contains the query
    if (item.getType().toString().toLowerCase().replace("_", " ").contains(query))
      return true;

    ItemMeta meta = item.getItemMeta();

    // Cannot check any further without a meta
    if (meta == null)
      return false;

    // Displayname contains the query
    if (ChatColor.stripColor(meta.getDisplayName()).toLowerCase().contains(query))
      return true;

    // Any of the lore lines contains the query
    return meta.getLore() != null && meta.getLore().stream().anyMatch(line -> (
      ChatColor.stripColor(line).toLowerCase().contains(query)
    ));
  }

  /**
   * Builds a query to select all auctions of a player
   * @param creator Target player
   * @param active Whether the auction should still be active
   */
  private QueryBuilder<AHAuctionModel> buildAuctionQuery(OfflinePlayer creator, boolean active) {
    QueryBuilder<AHAuctionModel> query = new QueryBuilder<>(
      AHAuctionModel.class,
      "creator__uuid", EqualityOperation.EQ, creator.getUniqueId()
    );

    if (active) {
      query.and(
        // Either still has a duration remaining
        new FieldQueryGroup("createdAt", FieldOperation.PLUS, "durationSeconds", EqualityOperation.GTE, System.currentTimeMillis() / 1000)
          // Or has no duration at all (instant buy)
          .or("durationSeconds", EqualityOperation.EQ, null)
      )
        // Has not yet been sold
        .and("sold", EqualityOperation.EQ, false);
    }

    return query;
  }

  /**
   * Builds a query to select the state of a player
   * @param owner Target player
   */
  private QueryBuilder<AHStateModel> buildStateQuery(OfflinePlayer owner) {
    return new QueryBuilder<>(
      AHStateModel.class,
      "owner__uuid", EqualityOperation.EQ, owner.getUniqueId()
    );
  }
}
