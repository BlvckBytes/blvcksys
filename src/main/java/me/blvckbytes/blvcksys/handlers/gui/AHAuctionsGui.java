package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.commands.IGiveCommand;
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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
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

  @AutoInjectLate
  private AHGui ahGui;

  @AutoInjectLate
  private AHProfileGui ahProfileGui;

  public AHAuctionsGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IAHHandler ahHandler,
    @AutoInject ConfirmationGui confirmationGui,
    @AutoInject IGiveCommand giveCommand
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_AUCTIONS_AH)
        .withVariable("name", i.getViewer().getName())
    ), plugin, cfg, textures);

    this.ahHandler = ahHandler;
    this.confirmationGui = confirmationGui;
    this.giveCommand = giveCommand;
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();
    Runnable back = () -> inst.switchTo(AnimationType.SLIDE_RIGHT, ahProfileGui, null);;

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);
    inst.addPagination(38, 40, 42);
    inst.addBack(36, e -> back.run());

    inst.setPageContents(() -> {
      // List all auctions this player has created, no matter if active or not
      List<AHAuctionModel> auctions = ahHandler.listCancellableOrRetrievableAuctions(p);

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

            // An auction just ended, refresh contents
            if (!auction.isActive())
              inst.refreshPageContents();

            AHBidModel lastBid = ahHandler.lastBid(auction, null).b();
            return ahGui.buildDisplayItem(
              p, auction, lastBid,
              cfg.get(
                auction.isActive() ?
                  // Auction is still active and may be deleted
                  ConfigKey.GUI_AH_AUCTION_LORE_DELETABLE :
                  (
                    lastBid == null ?
                    // Auction over, no bids, item retrievable
                    ConfigKey.GUI_AH_AUCTION_LORE_ITEMS_RETRIEVABLE :
                    // Auction over, bids, money retrievable
                    ConfigKey.GUI_AH_AUCTION_LORE_PRICE_RETRIEVABLE
                  )
              )
            );
          },
          e -> {
            if (e.getClick().isShiftClick() || !e.getClick().isLeftClick())
              return;

            // TODO: Don't confirm retrievals and handle possible states (items, price, delete) properly

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
              inst.reopen(AnimationType.SLIDE_RIGHT, confInst);
              inst.refreshPageContents();
            });
          },
          10 // Redraw every 1s/2 to guarantee proper synchronicity
        )
      ))
      .collect(Collectors.toList());
    });

    return true;
  }
}
