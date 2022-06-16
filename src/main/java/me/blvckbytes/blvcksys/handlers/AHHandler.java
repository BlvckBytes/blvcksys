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
import net.minecraft.util.Tuple;
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
import java.util.function.BiConsumer;
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

  // How many percent to increase the bid by each time
  private static final double BID_INCREASE_PERCENT = 0.05;

  private final IPersistence pers;
  private final Map<Player, AHStateModel> stateCache;
  private final Map<AHAuctionModel, List<AHBidModel>> auctionCache;
  private final List<Runnable> auctionDeltaInterests;
  private final List<BiConsumer<AHAuctionModel, AHBidModel>> bidInterests;

  public AHHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
    this.auctionDeltaInterests = new ArrayList<>();
    this.bidInterests = new ArrayList<>();
    this.stateCache = new HashMap<>();
    this.auctionCache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public AHStateModel getState(OfflinePlayer p) {
    // Respond from cache
    Player online = p.getPlayer();
    if (online != null && stateCache.containsKey(online))
      return stateCache.get(online);

    // Try to fetch from DB
    return pers.findFirst(buildStateQuery(p))
      // Not in DB yet
      .orElseGet(() -> {
        // Create, store and cache a new default instance
        AHStateModel def = AHStateModel.makeDefault(p);
        storeState(def);

        // Don't cache offline players
        if (online != null)
          stateCache.put(online, def);

        return def;
      });
  }

  @Override
  public void storeState(AHStateModel state) {
    pers.store(state);
  }

  @Override
  public int countActiveAuctions(Player creator) {
    return (int) auctionCache.keySet().stream()
      .filter(auction -> auction.isActive() && auction.comparePlayers(auction.getCreator(), creator))
      .count();
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

    AHAuctionModel auction = AHAuctionModel.makeDefault(creator, item, durationSeconds, startBid, category);
    pers.store(auction);
    auctionCache.put(auction, new ArrayList<>());
    auctionDeltaInterests.forEach(Runnable::run);
    return true;
  }

  @Override
  public boolean deleteAuction(AHAuctionModel auction) {
    boolean res = pers.delete(auction);
    auctionCache.remove(auction);
    auctionDeltaInterests.forEach(Runnable::run);
    return res;
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
  public TriResult retrieveAuctionMoney(OfflinePlayer executor, AHAuctionModel auction) {
    AHAuctionModel target = getAuctionById(auction.getId()).orElse(null);

    // Auction doesn't exist anymore
    if (target == null)
      return TriResult.EMPTY;

    // Still active or has no bids
    if (target.isActive() || lastBid(auction, null).b() == null)
      return TriResult.EMPTY;

    // Already payed out
    if (target.isPayed())
      return TriResult.ERR;

    target.setPayed(true);
    pers.store(target);
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
    if (!target.isActive())
      return TriResult.EMPTY;

    // Has been outbid already
    if (bids.size() != 0 && bids.get(bids.size() - 1).getAmount() >= amount)
      return TriResult.ERR;

    // Create the new bid
    AHBidModel bid = AHBidModel.makeDefault(executor, target, amount);
    pers.store(bid);
    bids.add(bid);
    bidInterests.forEach(interest -> interest.accept(auction, bid));

    return TriResult.SUCC;
  }

  @Override
  public List<AHAuctionModel> listPublicAuctions(
    AuctionCategory category, AuctionSort sort, @Nullable String searchQuery
  ) {
    return auctionCache.keySet().stream()
      .filter(auction -> {
        // Not active anymore
        if (!auction.isActive())
          return false;

        // Category mismatches and is not a wildcard
        if (category != AuctionCategory.ALL && !auction.getCategory().equals(category))
          return false;

        // Search mismatches and is not a wildcard
        return searchQuery == null || matchesSearch(auction.getItem(), searchQuery);
      })
      .sorted(auctionComparator(sort))
      .collect(Collectors.toList());
  }

  @Override
  public List<Tuple<AHAuctionModel, AHBidModel>> listParticipatingOrRetrievableBidAuctions(OfflinePlayer participant) {
    return auctionCache.entrySet().stream()
      // Map each auction to a list of bids from this participant
      .map(e -> new Tuple<>(e.getKey(), e.getValue().stream().filter(bid -> bid.comparePlayers(bid.getCreator(), participant)).collect(Collectors.toList())))
      // Filter out auctions where the player didn't bid on
      .filter(t -> t.b().size() > 0)
      // Get the last bid, as that will be the highest and represents what the player payed in total
      .map(t -> new Tuple<>(t.a(), t.b().get(t.b().size() - 1)))
      // Ignore already retrieved bids
      .filter(t -> !t.b().isRetrieved())
      // Sort by creation of the bid descending (newest first)
      .sorted((a, b) -> a.b().getCreatedAt().compareTo(b.b().getCreatedAt()))
      .collect(Collectors.toList());
  }

  @Override
  public List<AHAuctionModel> listPendingAuctions(OfflinePlayer creator) {
    return auctionCache.keySet().stream()
      .filter(auction -> {
        // Different creator
        if (!auction.comparePlayers(auction.getCreator(), creator))
          return false;

        // Money already retrieved
        if (auction.isPayed())
          return false;

        // Either active, retrievable, payable or cancellable
        return true;
      })
      .collect(Collectors.toList());
  }

  @Override
  public Optional<List<AHBidModel>> listBids(AHAuctionModel auction) {
    List<AHBidModel> bids = auctionCache.get(auction);

    if (bids == null)
      return Optional.empty();

    return Optional.of(bids);
  }

  @Override
  public Tuple<TriResult, @Nullable AHBidModel> lastBid(AHAuctionModel auction, @Nullable OfflinePlayer bidder) {
    List<AHBidModel> bids = listBids(auction).orElse(null);

    // Auction not existing
    if (bids == null)
      return new Tuple<>(TriResult.ERR, null);

    // Filter to respond last bid relative to bidder argument
    if (bidder != null) {
      bids = bids.stream()
        .filter(bid -> bid.comparePlayers(bid.getCreator(), bidder))
        .collect(Collectors.toList());
    }

    // No bids yet
    if (bids.size() == 0)
      return new Tuple<>(TriResult.EMPTY, null);

    // Return last bid
    return new Tuple<>(TriResult.SUCC, bids.get(bids.size() - 1));
  }

  @Override
  public Optional<Integer> nextBid(AHAuctionModel auction) {
    Tuple<TriResult, AHBidModel> currBid = lastBid(auction, null);

    // Auction unknown
    if (currBid.a() == TriResult.ERR)
      return Optional.empty();

    // Get the start bid if there are no bids yet
    if (currBid.b() == null)
      return Optional.of(auction.getStartBid());

    // Return the last bid + min step size
    return Optional.of((int) (currBid.b().getAmount() * (1 + BID_INCREASE_PERCENT)));
  }

  @Override
  public void registerBidInterest(BiConsumer<AHAuctionModel, AHBidModel> bid) {
    bidInterests.add(bid);
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
        .sorted(Comparator.comparingInt(AHBidModel::getAmount))
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
   * Comparator for sorting auctions based on the sorting parameters
   * @param sort Sorting parameters
   * @return Comparator to be used when sorting
   */
  private Comparator<AHAuctionModel> auctionComparator(AuctionSort sort) {
    return (a, b) -> {
      // TODO: Actually implement
      return 0;
    };
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
