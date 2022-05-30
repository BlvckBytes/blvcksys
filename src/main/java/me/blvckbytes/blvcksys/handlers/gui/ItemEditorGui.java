package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.SymbolicHead;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Edit all possible properties of an itemstack while always
  having a preview available.
*/
@AutoConstruct
public class ItemEditorGui extends AGui<ItemStack> {

  private final SingleChoiceGui singleChoiceGui;
  private final ILogger logger;
  private final ChatUtil chatUtil;

  public ItemEditorGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject SingleChoiceGui singleChoiceGui,
    @AutoInject ILogger logger,
    @AutoInject ChatUtil chatUtil
  ) {
    super(5, "", i -> (
      cfg.get(ConfigKey.GUI_ITEMEDITOR_TITLE)
        .withVariable("item_type", i.getArg().getType().name())
    ), plugin, cfg, textures);

    this.singleChoiceGui = singleChoiceGui;
    this.logger = logger;
    this.chatUtil = chatUtil;
  }

  @Override
  protected boolean closed(GuiInstance<ItemStack> inst) {
    return false;
  }

  @Override
  protected boolean opening(Player viewer, GuiInstance<ItemStack> inst) {
    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);

    ItemStack item = inst.getArg();
    ItemMeta meta = item.getItemMeta();
    Player p = inst.getViewer();

    if (meta == null) {
      p.sendMessage(
        cfg.get(ConfigKey.GUI_ITEMEDITOR_META_UNAVAILABLE)
          .withPrefix()
          .asScalar()
      );
      return false;
    }

    ///////////////////////////////////// Preview //////////////////////////////////////

    inst.fixedItem("12,14,22", i -> new ItemStackBuilder(Material.PURPLE_STAINED_GLASS_PANE).build(), null);
    inst.fixedItem(13, i -> item, null);

    ///////////////////////////////// Increase Amount //////////////////////////////////

    inst.fixedItem(10, i -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.ARROW_UP.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_AMOUNT_INCREASE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_AMOUNT_INCREASE_LORE))
        .build()
    ), e -> {
      ClickType click = e.getManipulation().getClick();
      int amount = item.getAmount();

      if (click.isLeftClick()) {
        if (click.isShiftClick())
          amount = item.getAmount() + 64;
        else
          amount = item.getAmount() + 1;
      }

      else if (click.isRightClick()) {
        if (click.isShiftClick())
          amount = 64;
        else
          amount = item.getAmount() + 8;
      }

      // Set the amount and redraw the display
      item.setAmount(amount);
      inst.redraw("13");

      p.sendMessage(
        cfg.get(ConfigKey.GUI_ITEMEDITOR_AMOUNT_CHANGED)
          .withPrefix()
          .withVariable("amount", amount)
          .asScalar()
      );
    });

    ///////////////////////////////// Decrease Amount //////////////////////////////////

    inst.fixedItem(16, i -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.ARROW_DOWN.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_AMOUNT_DECREASE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_AMOUNT_DECREASE_LORE))
        .build()
    ), e -> {
      ClickType click = e.getManipulation().getClick();
      int amount = item.getAmount();

      if (click.isLeftClick()) {
        if (click.isShiftClick())
          amount = item.getAmount() - 64;
        else
          amount = item.getAmount() - 1;
      }

      else if (click.isRightClick()) {
        if (click.isShiftClick())
          amount = 1;
        else
          amount = item.getAmount() - 8;
      }

      // Set the amount and redraw the display
      amount = Math.max(1, amount);
      item.setAmount(amount);
      inst.redraw("13");

      p.sendMessage(
        cfg.get(ConfigKey.GUI_ITEMEDITOR_AMOUNT_CHANGED)
          .withPrefix()
          .withVariable("amount", amount)
          .asScalar()
      );
    });

    ////////////////////////////////////// Commons //////////////////////////////////////

    // Inventory closed, re-open the editor
    Runnable closed = () -> Bukkit.getScheduler().runTaskLater(plugin, () -> this.show(p, item, AnimationType.SLIDE_UP), 1);

    // Back button
    Consumer<Inventory> backButton = inv -> this.show(viewer, item, AnimationType.SLIDE_RIGHT, inv);

    ///////////////////////////////////// Material /////////////////////////////////////

    inst.fixedItem(28, i -> (
      new ItemStackBuilder(Material.CHEST)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_MATERIAL_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_MATERIAL_LORE))
        .build()
    ), e -> {
      // Representitive items for each material
      List<Tuple<Object, ItemStack>> representitives = Arrays.stream(Material.values())
        .filter(m -> !(
          m.isAir() ||
          m.isLegacy()
        ))
        .map(m -> (
          new Tuple<>((Object) m, (
            new ItemStackBuilder(m)
              .withName(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_MATERIAL_NAME)
                  .withVariable(
                    "hr_type",
                    WordUtils.capitalizeFully(m.name().replace("_", " "))
                  )
              )
              .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_MATERIAL_LORE))
              .build()
          ))
        )
      ).toList();

      // Invoke a new single choice gui for available materials
      inst.switchTo(AnimationType.SLIDE_LEFT, singleChoiceGui, new SingleChoiceParam(
        cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_MATERIAL_TITLE).asScalar(), representitives,

        // Material selected
        (m, inv) -> {
          Material mat = (Material) m;

          item.setType(mat);
          this.show(p, item, AnimationType.SLIDE_RIGHT, inv);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_MATERIAL_CHANGED)
              .withPrefix()
              .withVariable("material", mat.name())
              .asScalar()
          );
          return false;
        }, closed, backButton
      ));
    });

    /////////////////////////////////// Item Flags ///////////////////////////////////

    inst.fixedItem(29, i -> (
      new ItemStackBuilder(Material.NAME_TAG)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_FLAGS_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_FLAGS_LORE))
        .build()
    ), e -> {
      // Representitive items for each flag
      List<Tuple<Object, ItemStack>> representitives = Arrays.stream(ItemFlag.values())
        .map(f -> {
          boolean has = meta.hasItemFlag(f);
          return new Tuple<>((Object) f, (
            new ItemStackBuilder(Material.NAME_TAG)
              .withName(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_NAME)
                  .withVariable("flag", f.name())
              )
              .withLore(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_LORE)
                  .withVariable(
                    "state",
                    cfg.get(has ? ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_ACTIVE : ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_INACTIVE)
                      .asScalar()
                  )
              )
              .build()
          ));
        }
      ).toList();

      // Invoke a new single choice gui for available flags
      inst.switchTo(AnimationType.SLIDE_LEFT, singleChoiceGui, new SingleChoiceParam(
        cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_TITLE).asScalar(), representitives,

        // Flag selected
        (f, inv) -> {
          ItemFlag flag = (ItemFlag) f;

          // Toggle the flag
          boolean has = meta.hasItemFlag(flag);
          if (has)
            meta.removeItemFlags(flag);
          else
            meta.addItemFlags(flag);

          item.setItemMeta(meta);

          this.show(p, item, AnimationType.SLIDE_RIGHT, inv);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_FLAG_CHANGED)
              .withPrefix()
              .withVariable("flag", flag.name())
              .withVariable(
                "state",
                cfg.get(!has ? ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_ACTIVE : ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_INACTIVE)
                  .asScalar()
              )
              .asScalar()
          );
          return false;
        }, closed, backButton
      ));
    });

    //////////////////////////////////// Enchantments ////////////////////////////////////

    inst.fixedItem(30, i -> (
      new ItemStackBuilder(Material.ENCHANTED_BOOK)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_ENCHANTMENTS_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_ENCHANTMENTS_LORE))
        .build()
    ), e -> {

      List<Enchantment> enchantments = new ArrayList<>();

      // Get all available enchantments from the abstract enchantment class's list of constant fields
      try {
        List<Field> constants = Arrays.stream(Enchantment.class.getDeclaredFields())
          .filter(field -> field.getType().equals(Enchantment.class) && Modifier.isStatic(field.getModifiers()))
          .toList();

        for (Field constant : constants)
          enchantments.add((Enchantment) constant.get(null));
      } catch (Exception ex) {
        logger.logError(ex);
      }

      // Representitive items for each enchantment
      List<Tuple<Object, ItemStack>> representitives = enchantments.stream()
        // Sort by relevance
        .sorted(Comparator.comparing(ench -> ench.canEnchantItem(item), Comparator.reverseOrder()))
        .map(ench -> {
            boolean has = meta.hasEnchant(ench);
            int level = -1;

            if (has)
              level = meta.getEnchantLevel(ench);

            return new Tuple<>((Object) ench, (
              new ItemStackBuilder(has ? Material.ENCHANTED_BOOK : Material.BOOK)
                .withEnchantment(ench, 1)
                .withName(
                  cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_NAME)
                    .withVariable(
                      "enchantment",
                      WordUtils.capitalizeFully(ench.getKey().getKey().replace("_", " "))
                    )
                )
                .withLore(
                  cfg.get(has ? ConfigKey.GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_LORE_ACTIVE : ConfigKey.GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_LORE_INACTIVE)
                    .withVariable(
                      "state",
                      cfg.get(has ? ConfigKey.GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_ACTIVE : ConfigKey.GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_INACTIVE)
                        .asScalar()
                    )
                    .withVariable("level", level)
                )
                .build()
            ));
          }
        ).toList();

      // Invoke a new single choice gui for available enchantments
      inst.switchTo(AnimationType.SLIDE_LEFT, singleChoiceGui, new SingleChoiceParam(
        cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_TITLE).asScalar(), representitives,

        // Enchantment selected
        (ench, inv) -> {
          Enchantment enchantment = (Enchantment) ench;
          boolean has = meta.hasEnchant(enchantment);

          // Undo the enchantment
          if (has) {
            meta.removeEnchant(enchantment);
            item.setItemMeta(meta);

            p.sendMessage(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_ENCHANTMENT_REMOVED)
                .withPrefix()
                .withVariable(
                  "enchantment",
                  WordUtils.capitalizeFully(enchantment.getKey().getKey().replace("_", " "))
                )
                .asScalar()
            );

            this.show(p, item, AnimationType.SLIDE_RIGHT, inv);
            return false;
          }

          // Prompt for the desired level in the chat
          chatUtil.registerPrompt(
            viewer,
            cfg.get(ConfigKey.GUI_ITEMEDITOR_ENCHANTMENT_LEVEL_PROMPT)
              .withPrefix()
              .asScalar(),

            // Level entered
            levelStr -> {

              // Parse the level from the string
              int level;
              try {
                level = Integer.parseInt(levelStr);
              } catch (NumberFormatException ex) {
                viewer.sendMessage(
                  cfg.get(ConfigKey.ERR_INTPARSE)
                    .withPrefix()
                    .withVariable("number", levelStr)
                    .asScalar()
                );

                this.show(p, item, AnimationType.SLIDE_UP);
                return;
              }

              meta.addEnchant(enchantment, level, true);
              item.setItemMeta(meta);

              p.sendMessage(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_ENCHANTMENT_ADDED)
                  .withPrefix()
                  .withVariable(
                    "enchantment",
                    WordUtils.capitalizeFully(enchantment.getKey().getKey().replace("_", " "))
                  )
                  .withVariable("level", level)
                  .asScalar()
              );

              this.show(p, item, AnimationType.SLIDE_UP);
            },

            closed
          );

          // Close the GUI when prompting for the chat message
          return true;
        }, closed, backButton
      ));
    });

    //////////////////////////////////// Displayname ////////////////////////////////////

    inst.fixedItem(31, i -> (
      new ItemStackBuilder(Material.PAPER)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_DISPLAYNAME_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_DISPLAYNAME_LORE))
        .build()
    ), e -> {

      // Prompt for the desired displayname in the chat
      chatUtil.registerPrompt(
        viewer,
        cfg.get(ConfigKey.GUI_ITEMEDITOR_DISPLAYNAME_PROMPT)
          .withPrefix()
          .asScalar(),

        // Name entered
        nameStr -> {
          boolean reset = nameStr.equalsIgnoreCase("null");
          nameStr = ChatColor.translateAlternateColorCodes('&', nameStr);

          meta.setDisplayName(reset ? null : nameStr);
          item.setItemMeta(meta);

          p.sendMessage(
            cfg.get(reset ? ConfigKey.GUI_ITEMEDITOR_DISPLAYNAME_RESET : ConfigKey.GUI_ITEMEDITOR_DISPLAYNAME_SET)
              .withPrefix()
              .withVariable("name", nameStr)
              .asScalar()
          );

          this.show(p, item, AnimationType.SLIDE_UP);
        },

        closed
      );

      // Close the GUI when prompting for the chat message
      e.getGui().close();
    });

    //////////////////////////////////// Lore Lines ////////////////////////////////////

    inst.fixedItem(32, i -> (
      new ItemStackBuilder(Material.OAK_SIGN)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_LORE))
        .build()
    ), e -> {
      // Insert after, insert at with RIGHT/LEFT click in a gui if there's more than one line

      ClickType click = e.getManipulation().getClick();

      if (click.isRightClick()) {
        // Reset the lore
        if (click.isShiftClick()) {

          // Set the lore and redraw the display
          meta.setLore(null);
          item.setItemMeta(meta);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_RESET)
              .withPrefix()
              .asScalar()
          );

          return;
        }

        // Remove specific line by choice
        List<String> lines = meta.getLore();

        // Has no lore yet
        if (lines == null) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_NO_LORE)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        // Offer a choice for which line to remove
        openLoreIndexChoice(lines, inst, (lineId, inv) -> {
          String content = lines.remove((int) lineId);
          meta.setLore(lines);
          item.setItemMeta(meta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_LINE_REMOVED)
              .withPrefix()
              .withVariable("line_number", lineId + 1)
              .withVariable("line_content", content)
              .asScalar()
          );

          this.show(p, item, AnimationType.SLIDE_RIGHT, inv);
          return false;
        }, closed, backButton);

        return;
      }

      // Add a new line
      if (click.isLeftClick()) {
        // Prompt for the desired lore line in the chat
        chatUtil.registerPrompt(
          viewer,
          cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_PROMPT)
            .withPrefix()
            .asScalar(),

          // Line entered
          loreStr -> {
            String lore = ChatColor.translateAlternateColorCodes('&', loreStr);

            List<String> lines = meta.getLore() == null ? new ArrayList<>() : meta.getLore();

            // There are no other lore lines yet, just add the line
            if (lines.size() == 0) {
              lines.add(lore);
              meta.setLore(lines);
              item.setItemMeta(meta);

              p.sendMessage(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_LINE_ADDED)
                  .withPrefix()
                  .asScalar()
              );

              this.show(p, item, AnimationType.SLIDE_UP);
              return;
            }

            // Add to the back of the list
            if (click.isShiftClick()) {
              lines.add(lore);

              meta.setLore(lines);
              item.setItemMeta(meta);

              p.sendMessage(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_LINE_ADDED)
                  .withPrefix()
                  .asScalar()
              );

              this.show(p, item, AnimationType.SLIDE_UP);
              return;
            }

            // Offer a choice for where to insert the line
            openLoreIndexChoice(lines, inst, (lineId, inv) -> {
              List<String> newLines = new ArrayList<>(lines);
              newLines.add(lineId, lore);

              meta.setLore(newLines);
              item.setItemMeta(meta);

              p.sendMessage(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_LINE_ADDED)
                  .withPrefix()
                  .asScalar()
              );

              this.show(p, item, AnimationType.SLIDE_RIGHT, inv);
              return false;
            }, closed, backButton);
          },

          closed
        );

        // Close the GUI when prompting for the chat message
        e.getGui().close();
      }
    });

    return true;
  }

  /**
   * Open a new lore line index choice GUI which presents the user with
   * an item for each line and then results in the index of the line chosen
   * @param lines Available lore lines
   * @param inst Gui instance to animate away from
   * @param chosen Chosen callback
   * @param closed Closed callback
   * @param backButton Back button callback
   */
  private void openLoreIndexChoice(
    List<String> lines,
    GuiInstance<ItemStack> inst,
    BiFunction<Integer, Inventory, Boolean> chosen,
    Runnable closed,
    Consumer<Inventory> backButton
  ) {
    // Create representitive items for each line
    List<Tuple<Object, ItemStack>> representitives = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      representitives.add(new Tuple<>(
        i,
        new ItemStackBuilder(Material.PAPER)
          .withName(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_LORE_NAME)
              .withVariable("line_number", i + 1)
          )
          .withLore(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_LORE_LORE)
              .withVariable("line_content", line)
          )
          .build()
      ));
    }

    // Invoke a new single choice gui for available lines
    inst.switchTo(AnimationType.SLIDE_LEFT, singleChoiceGui, new SingleChoiceParam(
      cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_LORE_TITLE).asScalar(),
      representitives,
      (m, inv) -> chosen.apply((int) m, inv),
      closed, backButton
    ));
  }
}
