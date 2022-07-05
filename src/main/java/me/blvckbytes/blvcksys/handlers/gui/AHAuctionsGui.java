package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.commands.IGiveCommand;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IAHHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerStatsHandler;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.persistence.models.AHAuctionModel;
import me.blvckbytes.blvcksys.persistence.models.AHBidModel;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/12/2022

  View all your currently active auctions and access their deletion screen.
*/
@AutoConstruct
public class AHAuctionsGui extends AGui<Object> {

  private final IAHHandler ahHandler;
  private final ConfirmationGui confirmationGui;
  private final IGiveCommand giveCommand;
  private final IPlayerStatsHandler playerStatsHandler;
  private final IStdGuiItemProvider stdGuiItemProvider;

  @AutoInjectLate
  private AHGui ahGui;

  @AutoInjectLate
  private AHProfileGui ahProfileGui;

  public AHAuctionsGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IAHHandler ahHandler,
    @AutoInject ConfirmationGui confirmationGui,
    @AutoInject IGiveCommand giveCommand,
    @AutoInject IPlayerStatsHandler playerStatsHandler,
    @AutoInject IStdGuiItemProvider stdGuiItemProvider
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_AUCTIONS_AH)
        .withVariable("name", i.getViewer().getName())
    ), plugin, cfg);

    this.ahHandler = ahHandler;
    this.confirmationGui = confirmationGui;
    this.giveCommand = giveCommand;
    this.playerStatsHandler = playerStatsHandler;
    this.stdGuiItemProvider = stdGuiItemProvider;
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();
    Runnable back = () -> inst.switchTo(AnimationType.SLIDE_RIGHT, ahProfileGui, null);;

    inst.addFill(stdGuiItemProvider);

    // Paginator
    inst.addPagination("38", "40", "42", stdGuiItemProvider);

    inst.addBack("36", stdGuiItemProvider, e -> back.run());

    inst.setPageContents(() -> {
      // List all auctions that are either active or require final interaction
      List<AHAuctionModel> auctions = ahHandler.listPendingAuctions(p);

      // No active auctions available
      if (auctions.size() == 0) {
        return List.of(
          new GuiItem(
            s -> (
              new ItemStackBuilder(Material.BARRIER)
                .withName(cfg.get(ConfigKey.GUI_AUCTIONS_AH_NONE_NAME))
                .withLore(cfg.get(ConfigKey.GUI_AUCTIONS_AH_NONE_LORE))
                .build()
            ), null, null
          )
        );
      }

      return auctions.stream().map(auction -> (
        new GuiItem(
          s -> {
            AHBidModel lastBid = ahHandler.lastBid(auction, null).b();

            TriResult state = getAuctionState(auction, lastBid).orElse(null);
            ConfigValue additionalLore = state == null ? null : switch (state) {
              case SUCC -> cfg.get(ConfigKey.GUI_AH_AUCTION_LORE_MONEY_RETRIEVABLE);
              case ERR -> cfg.get(ConfigKey.GUI_AH_AUCTION_LORE_ITEMS_RETRIEVABLE);
              case EMPTY -> cfg.get(ConfigKey.GUI_AH_AUCTION_LORE_CANCEL);
            };

            return ahGui.buildDisplayItem(p, auction, lastBid, null, additionalLore);
          },
          e -> {
            if (e.getClick().isShiftClick() || !e.getClick().isLeftClick())
              return;

            // Get the auction's state, ignore clicks if there's no action available
            AHBidModel lastBid = ahHandler.lastBid(auction, null).b();
            getAuctionState(auction, lastBid)
              .ifPresent(state -> {
                // The auction money is retrievable
                if (state == TriResult.SUCC && lastBid != null) {
                  TriResult res = ahHandler.retrieveAuctionMoney(p, auction);

                  // Successfully retrieved, hand out the money
                  if (res == TriResult.SUCC)
                    playerStatsHandler.addMoney(p, lastBid.getAmount());

                  p.sendMessage(
                    cfg.get(res == TriResult.SUCC ? ConfigKey.GUI_CREATE_AH_MONEY_RETRIEVED : ConfigKey.GUI_BID_AH_GONE)
                      .withPrefix()
                      .withVariable("money", lastBid.getAmount() + " Coins")
                      .asScalar()
                  );

                  inst.refreshPageContents();
                  return;
                }

                // The auction item is retrievable
                if (state == TriResult.ERR) {
                  boolean res = ahHandler.deleteAuction(auction);

                  // Successfully retrieved, hand back the item
                  if (res)
                    giveCommand.giveItemsOrDrop(p, auction.getItem());

                  p.sendMessage(
                    cfg.get(res ? ConfigKey.GUI_CREATE_AH_ITEM_RETRIEVED : ConfigKey.GUI_BID_AH_GONE)
                      .withPrefix()
                      .asScalar()
                  );

                  inst.refreshPageContents();
                  return;
                }

                // The auction can still be cancelled
                if (state == TriResult.EMPTY) {
                  // Prompt for deletion confirmation
                  inst.switchTo(AnimationType.SLIDE_LEFT, confirmationGui, (res, confInst) -> {
                    // Closed, do nothing
                    if (res == TriResult.EMPTY)
                      return;

                    // Confirmed deletion
                    if (res == TriResult.SUCC) {

                      // Delete the auction and thus check if it really existed
                      ItemStack item = auction.getItem();
                      boolean delRes = ahHandler.deleteAuction(auction);

                      // Give back the item of the auction if deletion succeeded
                      if (delRes) {
                        giveCommand.giveItemsOrDrop(p, item);
                        p.sendMessage(
                          cfg.get(ConfigKey.GUI_CREATE_AH_ITEM_RETURNED)
                            .withPrefix()
                            .asScalar()
                        );
                      }

                      // Notify of either deletion or noop
                      p.sendMessage(
                        cfg.get(delRes ? ConfigKey.GUI_AUCTIONS_AH_DELETED : ConfigKey.GUI_BID_AH_GONE)
                          .withPrefix()
                          .asScalar()
                      );
                    }

                    // Go back and update the page contents
                    inst.refreshPageContents();
                    inst.reopen(AnimationType.SLIDE_RIGHT, confInst);
                  });
                  return;
                }
              });
          },
          10 // Redraw every 1s/2 to guarantee proper synchronicity
        )
      ))
      .collect(Collectors.toList());
    });

    return true;
  }

  /**
   * Get the state an auction currently is in
   * @param auction Target auction
   * @param lastBid Last bid of the auction, null if there are no bids yet
   * @return SUCC if the creator can retrieve money, ERR if the items are to be
   * handed back (not sold), EMPTY if it's still cancellable
   */
  private Optional<TriResult> getAuctionState(AHAuctionModel auction, @Nullable AHBidModel lastBid) {
    // Has been sold but not payed out, money is retrievable
    if (!auction.isActive() && lastBid != null && !auction.isPayed())
      return Optional.of(TriResult.SUCC);

    // Has not been sold and isn't active anymore, items are retrievable
    if (!auction.isActive() && lastBid == null)
      return Optional.of(TriResult.ERR);

    // Auction is still active but there are no bids yet, still cancellable
    if (auction.isActive() && lastBid == null)
      return Optional.of(TriResult.EMPTY);

    return Optional.empty();
  }
}
