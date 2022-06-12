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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;
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

  @AutoInjectLate
  private AHProfileGui ahProfileGui;

  @AutoInjectLate
  private AHGui ahGui;

  public AHBidsGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IAHHandler ahHandler,
    @AutoInject AHBidGui ahBidGui
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_BIDS_AH)
        .withVariable("name", i.getViewer().getName())
    ), plugin, cfg, textures);

    this.ahHandler = ahHandler;
    this.ahBidGui = ahBidGui;
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
      List<Tuple<AHAuctionModel, Supplier<@Nullable AHBidModel>>> auctions = ahHandler.listAuctions(AuctionCategory.ALL, AuctionSort.NEWEST, null, p, null);

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

      // TODO: Decide whether the bid has expired and can be collected or if it's active and displays isHighest

      return auctions.stream().map(t -> (
        new GuiItem(
          s -> {

            // An auction just ended, refresh contents
            if (!t.a().isActive())
              inst.refreshPageContents();

            return ahGui.buildDisplayItem(t.a(), t.b().get());
          },
          e -> inst.switchTo(AnimationType.SLIDE_LEFT, ahBidGui, t.a()),
          10 // Redraw every 1s/2 to guarantee proper synchronicity
        )
      ))
      .collect(Collectors.toList());
    });

    return true;
  }
}
