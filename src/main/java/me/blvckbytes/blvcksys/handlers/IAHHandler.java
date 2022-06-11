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
import java.util.function.Consumer;
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
   */
  void deleteAuction(AHAuctionModel auction);

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
   * List all auctions based on a set of filters to apply
   * @param category Category to browse in
   * @param sort Direction to sort the results
   * @param searchQuery Query to use to search in names and lores as well as materials
   * @return List of results, tuple of auctions to their current bids (nullable for no bids)
   */
  List<Tuple<AHAuctionModel, Supplier<@Nullable AHBidModel>>> listAuctions(
    AuctionCategory category,
    AuctionSort sort,
    @Nullable String searchQuery
  );

  /**
   * List all bids of an auction
   * @param auctionId ID of the target auction
   */
  Optional<List<AHBidModel>> listBids(UUID auctionId);

  Optional<AHBidModel> lastBid(UUID auctionId);

  Optional<Integer> nextBid(UUID auctionId);

  /**
   * Register an interest for auction delta (new auctions, deleted auctions)
   * @param delta Callback to run after the delta
   */
  void registerAuctionDeltaInterest(Runnable delta);
}
