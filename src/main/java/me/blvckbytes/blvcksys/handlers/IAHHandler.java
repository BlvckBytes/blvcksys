package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.handlers.gui.AuctionCategory;
import me.blvckbytes.blvcksys.handlers.gui.AuctionSort;
import me.blvckbytes.blvcksys.persistence.models.AHAuctionModel;
import me.blvckbytes.blvcksys.persistence.models.AHBidModel;
import me.blvckbytes.blvcksys.persistence.models.AHStateModel;
import net.minecraft.util.Tuple;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/10/2022

  Public interfaces which the auction house handler provides to other consumers.
 */
public interface IAHHandler {

  /**
   * Get the current state of a given player
   * @param p Target player
   * @return Current state model
   */
  AHStateModel getState(OfflinePlayer p);

  /**
   * Store the current state for a given player
   * @param state Current state model
   */
  void storeState(AHStateModel state);

  /**
   * Count the number of currently active auctions of a given player
   * @param creator Target player
   * @return Number of active auctions
   */
  int countActiveAuctions(Player creator);

  /**
   * Get the maximum number of parallel auctions a player may have
   * @param creator Target player
   * @return Number of auctions the player may have
   */
  int getMaxAuctions(Player creator);

  /**
   * Create a new auction by it's parameters
   * @param creator Creating player
   * @param item Item of the auction
   * @param startBid Starting bid price
   * @param durationSeconds Duration in seconds
   * @param category Category of this item
   * @return True on success, false if this player reached their maximum number of parallel auctions
   */
  boolean createAuction(Player creator, ItemStack item, int startBid, int durationSeconds, AuctionCategory category);

  /**
   * Delete an existing auction
   * @param auction Auction to delete
   * @return Success state, false if the auction was already gone
   */
  boolean deleteAuction(AHAuctionModel auction);

  /**
   * Prematurely cancel an active auction as a moderator
   * @param executor Executing player
   * @param auction Auction to cancel
   * @return SUCC on success, EMPTY if the auction isn't active anymore and ERR if it's already cancelled
   */
  TriResult cancelAuction(OfflinePlayer executor, AHAuctionModel auction);

  /**
   * Create a new bid on an auction
   * @param executor Executing player
   * @param auction Auction to bid on
   * @param amount Amount to bid
   * @return SUCC on success, ERR if the amount has been outbid in the
   * mean time, EMPTY if this auction isn't active anymore
   */
  TriResult createBid(OfflinePlayer executor, AHAuctionModel auction, int amount);

  /**
   * List all publicly visible auctions which can still be bid on
   * @param category Selected category
   * @param sort Selected sorting
   * @param searchQuery Selected search query
   */
  List<AHAuctionModel> listPublicAuctions(
    AuctionCategory category, AuctionSort sort, @Nullable String searchQuery
  );

  /**
   * List all active auctions which the player is participating in or all past auctions
   * which the player didn't win and where money is still to be retrieved
   * @param participant Target player
   * @return List of tuples from auction to the player's latest bid on that auction
   */
  List<Tuple<AHAuctionModel, AHBidModel>> listParticipatingOrRetrievableBidAuctions(OfflinePlayer participant);

  /**
   * List all auctions which the player either may still cancel or where money is still to be retrieved
   * @param creator Target player
   */
  List<AHAuctionModel> listCancellableOrRetrievableAuctions(OfflinePlayer creator);

  /**
   * List all bids of an auction
   * @param auction Target auction
   */
  Optional<List<AHBidModel>> listBids(AHAuctionModel auction);

  /**
   * Get the last bid from an auction
   * @param auction Target auction
   * @param bidder Optional player to respond relative to
   * @return Last bid if exists (SUCC), EMPTY if there are no bids yet, ERR if the auction is unknown
   */
  Tuple<TriResult, @Nullable AHBidModel> lastBid(AHAuctionModel auction, @Nullable OfflinePlayer bidder);

  /**
   * Get the next lowest bid on an auction
   * @param auction Target auction
   * @return Next lowest bid, empty if the auction is unknown
   */
  Optional<Integer> nextBid(AHAuctionModel auction);

  /**
   * Register an interest for auction delta (new auctions, deleted auctions)
   * @param delta Callback to run after the delta
   */
  void registerAuctionDeltaInterest(Runnable delta);

  /**
   * Register an interest for new bids on auctions
   * @param bid Callback to run after a new bid came in,
   *            containing the affected auction and the new bid
   */
  void registerBidInterest(BiConsumer<AHAuctionModel, AHBidModel> bid);
}
