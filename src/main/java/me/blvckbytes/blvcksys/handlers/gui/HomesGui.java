package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHomeHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.persistence.models.HomeModel;
import net.minecraft.util.Tuple;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/07/2022

  Manage all homes by either deleting them, updating their location or
  choosing a new representitive icon.
*/
@AutoConstruct
public class HomesGui extends AGui<OfflinePlayer> {

  private final IHomeHandler homeHandler;
  private final ConfirmationGui confirmationGui;
  private final SingleChoiceGui singleChoiceGui;
  private final ItemEditorGui itemEditorGui;

  public HomesGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IHomeHandler homeHandler,
    @AutoInject ConfirmationGui confirmationGui,
    @AutoInject SingleChoiceGui singleChoiceGui,
    @AutoInject ItemEditorGui itemEditorGui
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_HOMES)
        .withVariable("name", i.getArg().getName())
    ), plugin, cfg, textures);

    this.homeHandler = homeHandler;
    this.confirmationGui = confirmationGui;
    this.singleChoiceGui = singleChoiceGui;
    this.itemEditorGui = itemEditorGui;
  }

  @Override
  protected boolean closed(GuiInstance<OfflinePlayer> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<OfflinePlayer> inst) {
    Player p = inst.getViewer();
    boolean isSelf = inst.getArg().equals(p);

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);
    inst.addPagination(37, 40, 43);

    inst.setPageContents(() -> {
      List<HomeModel> homes = homeHandler.listHomes(inst.getArg());

      // No homes available
      if (homes.size() == 0) {
        return List.of(
          new GuiItem(s -> (
            new ItemStackBuilder(Material.BARRIER)
              .withName(cfg.get(ConfigKey.GUI_HOMES_NONE_NAME))
              .withLore(
                cfg.get(isSelf ? ConfigKey.GUI_HOMES_NONE_LORE_SELF : ConfigKey.GUI_HOMES_NONE_LORE_OTHERS)
                  .withVariable("name", inst.getArg().getName())
              )
              .build()
            ), null, null
          )
        );
      }

      return homes.stream()
        .map(home -> {

          Location l = home.getLoc();
          World w = l.getWorld();

          return new GuiItem(
            s -> (
              new ItemStackBuilder(home.getIcon())
                .withName(
                  cfg.get(ConfigKey.GUI_HOMES_HOME_NAME)
                    .withVariable("name", home.getName())
                )
                .withLore(
                  cfg.get(ConfigKey.GUI_HOMES_HOME_LORE)
                    .withVariable("color", home.getColor())
                    .withVariable("created_at", home.getCreatedAtStr())
                    .withVariable("updated_at", home.getUpdatedAtStr())
                    .withVariable("world", w == null ? "/" : w.getName())
                    .withVariable("location", "(" + l.getBlockX() + " | " + l.getBlockY() + " | " + l.getBlockZ() + ")")
                )
                .build()
            ),
            e -> {

              if (e.getClick().isLeftClick() && !e.getClick().isShiftClick()) {
                // Teleport to the home
                inst.close();
                p.performCommand(
                  "home " + home.getName() + (isSelf ? "" : " " + inst.getArg().getName())
                );

                return;
              }

              e.getHotbarKey().ifPresent(key -> {
                switch (key) {
                  case 1 -> chooseIcon(inst, home);
                  case 2 -> moveHome(inst, home);
                  case 3 -> chooseColor(inst, home);
                  case 4 -> deleteHome(inst, home);
                }
              });
            }, null);
        })
        .collect(Collectors.toList());
    });

    return true;
  }

  private void chooseIcon(GuiInstance<OfflinePlayer> inst, HomeModel home) {
    new UserInputChain(inst, values -> {
      Material mat = (Material) values.get("material");
      boolean succ = homeHandler.updateIcon(inst.getArg(), home.getName(), mat);

      inst.getViewer().sendMessage(
        cfg.get(succ ? ConfigKey.GUI_HOMES_ICON_CHANGED : ConfigKey.HOMES_NOT_FOUND)
          .withPrefix()
          .withVariable("material", formatConstant(mat.name()))
          .asScalar()
      );
    }, singleChoiceGui, null)
      .withChoice(
        "material",
        cfg.get(ConfigKey.GUI_HOMES_CHOICE_ICON_TITLE),
        itemEditorGui::buildMaterialRepresentitives,
        null
      )
      .start();
  }

  private void moveHome(GuiInstance<OfflinePlayer> inst, HomeModel home) {
    Player p = inst.getViewer();

    // Re-set the home at the current position
    inst.switchTo(AnimationType.SLIDE_LEFT, confirmationGui, (res, confirmationInst) -> {
      // Closed the inventory, take no action
      if (res == TriResult.EMPTY)
        return;

      // Perform update on confirmation
      if (res == TriResult.SUCC) {
        boolean succ = homeHandler.updateLocation(inst.getArg(), home.getName(), p.getLocation());
        p.sendMessage(
          cfg.get(succ ? ConfigKey.HOMES_MOVED : ConfigKey.HOMES_NOT_FOUND)
            .withPrefix()
            .withVariable("name", home.getName())
            .asScalar()
        );
      }

      // In any case, switch back to the list
      confirmationInst.switchTo(AnimationType.SLIDE_RIGHT, this, inst.getArg());
    });
  }

  private void chooseColor(GuiInstance<OfflinePlayer> inst, HomeModel home) {
    new UserInputChain(inst, values -> {
      ChatColor color = (ChatColor) values.get("color");
      boolean succ = homeHandler.updateColor(inst.getArg(), home.getName(), color);

      inst.getViewer().sendMessage(
        cfg.get(succ ? ConfigKey.GUI_HOMES_COLOR_CHANGED : ConfigKey.HOMES_NOT_FOUND)
          .withPrefix()
          .withVariable("color", color)
          .asScalar()
      );
    }, singleChoiceGui, null)
      .withChoice(
        "color",
        cfg.get(ConfigKey.GUI_HOMES_CHOICE_COLOR_TITLE),
        this::buildChatColorRepresentitives,
        null
      )
      .start();
  }

  /**
   * Build a list of representitives for all available chat colors
   */
  public List<Tuple<Object, ItemStack>> buildChatColorRepresentitives() {
    // Representitive items for each color
    return Arrays.stream(ChatColor.values())
      // Filter out formats
      .filter(ChatColor::isColor)
      .map(color -> (
          new Tuple<>((Object) color, (
            new ItemStackBuilder(chatColorToMaterial(color))
              .withName(
                cfg.get(ConfigKey.GUI_HOMES_CHOICE_COLOR_NAME)
                  .withVariable("name", formatConstant(color.name()))
              )
              .withLore(
                cfg.get(ConfigKey.GUI_HOMES_CHOICE_COLOR_LORE)
                  .withVariable("color", color)
              )
              .build()
          ))
        )
      ).toList();
  }

  /**
   * Translates chat colors to a representitive material (block)
   * @param color Color to translate
   * @return Representitive block
   */
  private Material chatColorToMaterial(ChatColor color) {
    return switch (color) {
      case BLACK -> Material.BLACK_TERRACOTTA;
      case DARK_BLUE -> Material.BLUE_TERRACOTTA;
      case DARK_GREEN -> Material.GREEN_TERRACOTTA;
      case DARK_AQUA -> Material.LIGHT_BLUE_TERRACOTTA;
      case RED, DARK_RED -> Material.RED_TERRACOTTA;
      case DARK_PURPLE -> Material.PURPLE_TERRACOTTA;
      case GOLD -> Material.ORANGE_TERRACOTTA;
      case GRAY -> Material.LIGHT_GRAY_TERRACOTTA;
      case DARK_GRAY -> Material.GRAY_TERRACOTTA;
      case GREEN -> Material.LIME_TERRACOTTA;
      case BLUE, AQUA -> Material.CYAN_TERRACOTTA;
      case LIGHT_PURPLE -> Material.PINK_TERRACOTTA;
      case YELLOW -> Material.YELLOW_TERRACOTTA;
      default -> Material.WHITE_TERRACOTTA;
    };
  }

  private void deleteHome(GuiInstance<OfflinePlayer> inst, HomeModel home) {
    Player p = inst.getViewer();

    // Delete this home
    inst.switchTo(AnimationType.SLIDE_LEFT, confirmationGui, (res, confirmationInst) -> {
      // Closed the inventory, take no action
      if (res == TriResult.EMPTY)
        return;

      // Perform deletion
      if (res == TriResult.SUCC) {
        boolean succ = homeHandler.deleteHome(inst.getArg(), home.getName());
        p.sendMessage(
          cfg.get(succ ? ConfigKey.HOMES_DELETED : ConfigKey.HOMES_NOT_FOUND)
            .withPrefix()
            .withVariable("name", home.getName())
            .asScalar()
        );
      }

      // In any case, switch back to the list
      confirmationInst.switchTo(AnimationType.SLIDE_RIGHT, this, inst.getArg());
    });
  }
}