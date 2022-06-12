package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IAHHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.persistence.models.AHAuctionModel;
import me.blvckbytes.blvcksys.persistence.models.AHBidModel;
import me.blvckbytes.blvcksys.util.ChatUtil;
import net.minecraft.util.Tuple;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/11/2022

  Place a new bid on a previously selected auction and watch
  that specific auction in detail.
*/
@AutoConstruct
public class AHBidGui extends AGui<AHAuctionModel> {

  // Maximum number of lines (bids) in the bid history lore
  private static final int BID_HISTORY_MAXLINES = 10;

  private final ChatUtil chatUtil;
  private final IAHHandler ahHandler;

  @AutoInjectLate
  private AHGui ahGui;

  public AHBidGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IAHHandler ahHandler,
    @AutoInject ChatUtil chatUtil
  ) {
    super(3, "", i -> (
      cfg.get(ConfigKey.GUI_BID_AH)
    ), plugin, cfg, textures);

    this.ahHandler = ahHandler;
    this.chatUtil = chatUtil;

    // Redraw the GUI if it contains an auction which has just received a new bid
    ahHandler.registerBidInterest((auction, bid) -> {
      getActiveInstances().values().forEach(insts -> {
        insts.forEach(inst -> {
          // Different auction, ignore
          if (!inst.getArg().equals(auction))
            return;

          // Redraw affected slots (status, item, min-bid, log)
          inst.redraw("2,20,11,15,26");
        });
      });
    });
  }

  @Override
  protected boolean closed(GuiInstance<AHAuctionModel> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<AHAuctionModel> inst) {
    Player p = inst.getViewer();
    AHAuctionModel auction = inst.getArg();

    Runnable back = () -> inst.switchTo(AnimationType.SLIDE_RIGHT, ahGui, null);;

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);
    inst.addBack(18, e -> back.run());

    // Target item status indicators
    inst.fixedItem("2,20", () -> createStatusIndicator(inst).orElse(null), null);

    // Target item itself
    inst.fixedItem(11, () -> {
      Tuple<TriResult, @Nullable AHBidModel> lastBid = ahHandler.lastBid(auction.getId());

      // Auction not available anymore, close the screen
      if (lastBid.a() == TriResult.ERR) {
        p.sendMessage(
          cfg.get(ConfigKey.GUI_BID_AH_GONE)
            .withPrefix()
            .asScalar()
        );
        back.run();
      }

      return ahGui.buildDisplayItem(auction, lastBid.b());
    }, null, 10);

    // Custom bid
    inst.fixedItem(13, () -> (
      new ItemStackBuilder(Material.OAK_SIGN)
        .withName(cfg.get(ConfigKey.GUI_BID_AH_CUSTOM_BID_NAME))
        .withLore(cfg.get(ConfigKey.GUI_BID_AH_CUSTOM_BID_LORE))
        .build()
      ),
      e -> {
        new UserInputChain(inst, values -> {
          // Cannot request negative or zero amounts
          int amount = (int) values.get("startBid");
          if (amount <= 0) {
            p.sendMessage(
              cfg.get(ConfigKey.GUI_BID_AH_CUSTOM_BID_PROMPT_INVALID)
                .withPrefix()
                .asScalar()
            );
            return;
          }

          bidAmount(inst, amount, back);
        }, null, chatUtil)
          .withPrompt(
            "startBid",
            values -> cfg.get(ConfigKey.GUI_BID_AH_CUSTOM_BID_PROMPT_MESSAGE),
            // Has to be an integer number
            Integer::parseInt,
            input -> cfg.get(ConfigKey.ERR_INTPARSE)
              .withPrefix()
              .withVariable("number", input),
            null
          )
          .start();
      },
      null
    );

    // Minimum bid
    inst.fixedItem(15, () -> (
      new ItemStackBuilder(Material.BOOK)
        .withName(cfg.get(ConfigKey.GUI_BID_AH_MIN_BID_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_BID_AH_MIN_BID_LORE)
            .withVariable("next_bid", ahHandler.nextBid(auction.getId()).orElseGet(auction::getStartBid) + " Coins")
        )
        .build()
      ),
      e -> {
        int nextBid = ahHandler.nextBid(auction.getId()).orElseGet(auction::getStartBid);
        bidAmount(inst, nextBid, back);
      }
    );

    // Bidding history log
    inst.fixedItem(26, () -> {
      StringBuilder lines = new StringBuilder();
      List<AHBidModel> bids = ahHandler.listBids(auction.getId()).orElse(new ArrayList<>());

      if (bids.size() == 0)
        lines.append(cfg.get(ConfigKey.GUI_BID_AH_BID_HISTORY_NONE).asScalar());

      else {
        for (int i = bids.size() - 1; i >= Math.max(0, bids.size() - BID_HISTORY_MAXLINES); i--) {
          AHBidModel bid = bids.get(i);

          lines.append(i == bids.size() - 1 ? "" : "\n").append(
            cfg.get(ConfigKey.GUI_BID_AH_BID_HISTORY_LINE)
              .withVariable("bidder", bid.getCreator().getName())
              .withVariable("bid", bid.getAmount())
              .withVariable("date", bid.getCreatedAtStr())
              .asScalar()
          );
        }
      }

      return new ItemStackBuilder(Material.PAPER)
        .withName(cfg.get(ConfigKey.GUI_BID_AH_BID_HISTORY_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_BID_AH_BID_HISTORY_LORE)
            .withVariable("history_lines", lines)
        )
        .build();
    }, null);

    return true;
  }

  /**
   * Tries to bid the given amount and notifies the player if
   * they've been outbid or backs out if the auction isn't available anymore
   * @param inst GUI ref
   * @param amount Amount to bid
   * @param back Executed when the auction is invalid
   */
  private void bidAmount(GuiInstance<AHAuctionModel> inst, int amount, Runnable back) {
    Player p = inst.getViewer();
    AHAuctionModel auction = inst.getArg();

    // Cannot bid on their own auction
    if (auction.getCreator().equals(p)) {
      p.sendMessage(
        cfg.get(ConfigKey.GUI_BID_AH_BID_SELF)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    Tuple<TriResult, @Nullable AHBidModel> lastBid = ahHandler.lastBid(auction.getId());
    AHBidModel bid = lastBid.b();
    TriResult res = ahHandler.createBid(p, auction, amount);
    Optional<Integer> nextBid = ahHandler.nextBid(auction.getId());

    // Auction has been invalidated in the meantime
    if (res == TriResult.EMPTY || nextBid.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.GUI_BID_AH_GONE)
          .withPrefix()
          .asScalar()
      );
      back.run();
      return;
    }

    // Already outbid before placing the bid
    if (res == TriResult.ERR) {
      p.sendMessage(
        cfg.get(ConfigKey.GUI_BID_AH_CUSTOM_BID_PROMPT_OUTBID)
          .withPrefix()
          .withVariable("last_bid", bid == null ? "/" : (bid.getAmount() + " Coins"))
          .withVariable("min_bid", nextBid.get() + " Coins")
          .asScalar()
      );
      return;
    }

    // Bid placed successfully
    p.sendMessage(
      cfg.get(ConfigKey.GUI_BID_AH_BID_PLACED)
        .withPrefix()
        .withVariable("bid", amount + " Coins")
        .asScalar()
    );

    // Notify the previously highest bidding player, if applicable
    if (bid != null) {
      // Last bidder isn't online anymore
      if (!bid.getCreator().isOnline())
        return;

      Player bidder = (Player) bid.getCreator();

      // Outbid themselves (why?)
      if (bidder.equals(p))
        return;

      bidder.sendMessage(
        cfg.get(ConfigKey.GUI_BID_AH_BID_OUTBID)
          .withPrefix()
          .withVariable("bid", bid.getAmount() + " Coins")
          .withVariable("outbidder", p.getName())
          .withVariable("new_bid", amount + " Coins")
          .asScalar()
      );
    }
  }

  /**
   * Creates the status indicators to be placed above and below the target auction
   * item, which quickly signal the viewer's bidding status within the auction
   * @param inst GUI ref
   * @return Optional indicator, empty if the auction could not be found
   */
  private Optional<ItemStack> createStatusIndicator(GuiInstance<AHAuctionModel> inst) {
    List<AHBidModel> bids = ahHandler.listBids(inst.getArg().getId()).orElse(null);

    // Could not find the target auction
    if (bids == null)
      return Optional.empty();

    boolean isBidding = bids.stream().anyMatch(bid -> bid.getCreator().equals(inst.getViewer()));
    boolean isHighest = bids.size() > 0 && bids.get(bids.size() - 1).getCreator().equals(inst.getViewer());

      return Optional.of(
        new ItemStackBuilder(
          isBidding ?
            (isHighest ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE) :
            Material.ORANGE_STAINED_GLASS_PANE
        )
          .withName(cfg.get(
            isBidding ?
              (isHighest ? ConfigKey.GUI_BID_AH_IS_HIGHEST_NAME : ConfigKey.GUI_BID_AH_OUTBID_NAME) :
              ConfigKey.GUI_BID_AH_NOT_BIDDING_NAME
          ))
          .withLore(cfg.get(
            isBidding ?
              (isHighest ? ConfigKey.GUI_BID_AH_IS_HIGHEST_LORE : ConfigKey.GUI_BID_AH_OUTBID_LORE) :
              ConfigKey.GUI_BID_AH_NOT_BIDDING_LORE
          ))
        .build()
    );
  }
}
