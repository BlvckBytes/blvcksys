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
import net.minecraft.util.Tuple;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/12/2022

  View all your currently active bids and quickly access
  the bidding screen through them.
*/
@AutoConstruct
public class AHBidsGui extends AGui<Object> {

  private final IAHHandler ahHandler;
  private final AHBidGui ahBidGui;
  private final IStdGuiItemsProvider stdGuiItemsProvider;

  @AutoInjectLate
  private AHProfileGui ahProfileGui;

  @AutoInjectLate
  private AHGui ahGui;

  public AHBidsGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IAHHandler ahHandler,
    @AutoInject AHBidGui ahBidGui,
    @AutoInject IStdGuiItemsProvider stdGuiItemsProvider
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_BIDS_AH)
        .withVariable("name", i.getViewer().getName())
    ), plugin, cfg, textures);

    this.ahHandler = ahHandler;
    this.ahBidGui = ahBidGui;
    this.stdGuiItemsProvider = stdGuiItemsProvider;
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();
    Runnable back = () -> inst.switchTo(AnimationType.SLIDE_RIGHT, ahProfileGui, null);;

    inst.addFill(stdGuiItemsProvider);

    // Paginator
    inst.addPagination("38", "40", "42", stdGuiItemsProvider);

    inst.addBack("36", stdGuiItemsProvider, e -> back.run());

    inst.setPageContents(() -> {
      List<Tuple<AHAuctionModel, AHBidModel>> auctions = ahHandler.listParticipatingOrRetrievableBidAuctions(p);

      // No active bids available
      if (auctions.size() == 0) {
        return List.of(
          new GuiItem(
            s -> (
              new ItemStackBuilder(Material.BARRIER)
                .withName(cfg.get(ConfigKey.GUI_BIDS_AH_NONE_NAME))
                .withLore(cfg.get(ConfigKey.GUI_BIDS_AH_NONE_LORE))
                .build()
            ), null, null
          )
        );
      }

      return auctions.stream().map(t -> (
        new GuiItem(
          s -> {
            // An auction just ended, refresh contents
            if (!t.a().isActive())
              inst.refreshPageContents();

            AHBidModel lastBid = ahHandler.lastBid(t.a(), null).b();
            AHBidModel viewerBid = ahHandler.lastBid(t.a(), p).b();
            return ahGui.buildDisplayItem(p, t.a(), lastBid, viewerBid, null);
          },
          e -> {
            // TODO: Handle retrieval- or new bid states properly

            // Auction is still active, go to the bid gui
            if (t.a().isActive()) {
              inst.switchTo(AnimationType.SLIDE_LEFT, ahBidGui, t.a());
              return;
            }

            // Already retrieved, should not be displayed here anyways
            if (t.b().isRetrieved())
              return;

            p.sendMessage("§aWould now retrieve " + t.b().getAmount() + " Coins");
          },
          10 // Redraw every 1s/2 to guarantee proper synchronicity
        )
      ))
      .collect(Collectors.toList());
    });

    return true;
  }
}
