package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IAHHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.AHAuctionModel;
import me.blvckbytes.blvcksys.persistence.models.AHBidModel;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

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

  private final IAHHandler ahHandler;

  @AutoInjectLate
  private AHGui ahGui;

  public AHBidGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IAHHandler ahHandler
  ) {
    super(3, "", i -> (
      cfg.get(ConfigKey.GUI_BID_AH)
    ), plugin, cfg, textures);

    this.ahHandler = ahHandler;
  }

  @Override
  protected boolean closed(GuiInstance<AHAuctionModel> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<AHAuctionModel> inst) {
    Player p = inst.getViewer();
    AHAuctionModel auction = inst.getArg();

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);
    inst.addBack(18, ahGui, null, AnimationType.SLIDE_RIGHT);

    // Target item status indicators
    inst.fixedItem("2,20", () -> {
      ItemStack item = createStatusIndicator(inst).orElse(null);

      if (item == null) {
        p.sendMessage("§cAuction is gone");
        inst.close();
      }

      return item;
    }, null);

    // Target item itself
    inst.fixedItem(11, () -> {
      List<AHBidModel> bids = ahHandler.listBids(auction.getId()).orElse(new ArrayList<>());
      return ahGui.buildDisplayItem(auction, bids.size() == 0 ? null : bids.get(bids.size() - 1));
    }, null, 10);

    // Custom bid
    inst.fixedItem(13, () -> (
      new ItemStackBuilder(Material.OAK_SIGN)
        .withName(cfg.get(ConfigKey.GUI_BID_AH_CUSTOM_BID_NAME))
        .withLore(cfg.get(ConfigKey.GUI_BID_AH_CUSTOM_BID_LORE))
        .build()
      ),
      e -> {},
      null
    );

    // Minimum bid
    inst.fixedItem(15, () -> (
      new ItemStackBuilder(Material.BOOK)
        .withName(cfg.get(ConfigKey.GUI_BID_AH_MIN_BID_NAME))
        .withLore(cfg.get(ConfigKey.GUI_BID_AH_MIN_BID_LORE))
        .build()
      ),
      e -> {},
      null
    );

    return true;
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
