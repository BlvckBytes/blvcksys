package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.util.SymbolicHead;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/10/2022

  The home screen of a player's auction house profile.
*/
@AutoConstruct
public class AHProfileGui extends AGui<Object> {

  private final AHCreateGui ahCreateGui;
  private final AHBidsGui ahBidsGui;
  private final AHAuctionsGui ahAuctionsGui;
  private final IStdGuiItemProvider stdGuiItemProvider;
  private final IPlayerTextureHandler textures;

  @AutoInjectLate
  private AHGui ahGui;

  public AHProfileGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject AHCreateGui ahCreateGui,
    @AutoInject AHBidsGui ahBidsGui,
    @AutoInject AHAuctionsGui ahAuctionsGui,
    @AutoInject IStdGuiItemProvider stdGuiItemProvider
  ) {
    super(3, "", i -> (
      cfg.get(ConfigKey.GUI_PROFILE_AH)
        .withVariable("name", i.getViewer().getName())
    ), plugin, cfg);

    this.textures = textures;
    this.ahCreateGui = ahCreateGui;
    this.ahBidsGui = ahBidsGui;
    this.ahAuctionsGui = ahAuctionsGui;
    this.stdGuiItemProvider = stdGuiItemProvider;
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {

    inst.addFill(stdGuiItemProvider);

    inst.addBack(
      "18", stdGuiItemProvider, ahGui, null, AnimationType.SLIDE_RIGHT
    );

    // New entry
    inst.fixedItem("11", () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.GREEN_PLUS.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_PROFILE_AH_CREATE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_PROFILE_AH_CREATE_LORE))
        .build()
    ), e -> inst.switchTo(AnimationType.SLIDE_LEFT, ahCreateGui, null), null);

    // Manage auctions
    inst.fixedItem("13", () -> (
      new ItemStackBuilder(Material.ANVIL)
        .withName(cfg.get(ConfigKey.GUI_PROFILE_AH_MANAGE_AUCTIONS_NAME))
        .withLore(cfg.get(ConfigKey.GUI_PROFILE_AH_MANAGE_AUCTIONS_LORE))
        .build()
    ), e -> inst.switchTo(AnimationType.SLIDE_LEFT, ahAuctionsGui, null), null);

    // Manage bids
    inst.fixedItem("15", () -> (
      new ItemStackBuilder(Material.CLOCK)
        .withName(cfg.get(ConfigKey.GUI_PROFILE_AH_MANAGE_BIDS_NAME))
        .withLore(cfg.get(ConfigKey.GUI_PROFILE_AH_MANAGE_BIDS_LORE))
        .build()
    ), e -> inst.switchTo(AnimationType.SLIDE_LEFT, ahBidsGui, null), null);

    return true;
  }
}
