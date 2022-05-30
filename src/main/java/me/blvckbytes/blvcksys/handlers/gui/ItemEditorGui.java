package me.blvckbytes.blvcksys.handlers.gui;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.PlayerTextureModel;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.SymbolicHead;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Edit all possible properties of an itemstack while always
  having a preview available.
  Args: Item to edit, item change callback, back button callback
*/
@AutoConstruct
public class ItemEditorGui extends AGui<Triple<ItemStack, @Nullable Consumer<ItemStack>, @Nullable Consumer<Inventory>>> {

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
        .withVariable("item_type", i.getArg().a().getType().name())
    ), plugin, cfg, textures);

    this.singleChoiceGui = singleChoiceGui;
    this.logger = logger;
    this.chatUtil = chatUtil;
  }

  @Override
  protected boolean closed(GuiInstance<Triple<ItemStack, @Nullable Consumer<ItemStack>, @Nullable Consumer<Inventory>>> inst) {
    return false;
  }

  @Override
  protected boolean opening(Player viewer, GuiInstance<Triple<ItemStack, @Nullable Consumer<ItemStack>, @Nullable Consumer<Inventory>>> inst) {
    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);

    ItemStack item = inst.getArg().a();
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

    /////////////////////////////////// Back Button ////////////////////////////////////

    // Only render the back button if a callback has been provided
    Consumer<Inventory> back = inst.getArg().c();
    if (back != null) {
      inst.addBack(36, i -> back.accept(i.getGui().getInv()));
    }

    ///////////////////////////////////// Preview //////////////////////////////////////

    inst.fixedItem("12,14", i -> new ItemStackBuilder(Material.PURPLE_STAINED_GLASS_PANE).build(), null);
    inst.fixedItem(13, i -> item, null);

    // Fire the item update callback whenever the preview slot changes
    inst.onRedrawing(13, () -> {
      Consumer<ItemStack> cb = inst.getArg().b();
      if (cb != null)
        cb.accept(item);
    });

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
    Runnable closed = () -> Bukkit.getScheduler().runTaskLater(plugin, () -> this.show(p, inst.getArg(), AnimationType.SLIDE_UP), 1);

    // Back button
    Consumer<Inventory> backButton = inv -> this.show(viewer, inst.getArg(), AnimationType.SLIDE_RIGHT, inv);

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
                  .withVariable("hr_type", formatConstant(m.name()))
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
          this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, inv);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_MATERIAL_CHANGED)
              .withPrefix()
              .withVariable("material", formatConstant(mat.name()))
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
                  .withVariable("flag", formatConstant(f.name()))
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

          this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, inv);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_FLAG_CHANGED)
              .withPrefix()
              .withVariable("flag", formatConstant(flag.name()))
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
                    .withVariable("enchantment", formatConstant(ench.getKey().getKey()))
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
                .withVariable("enchantment", formatConstant(enchantment.getKey().getKey()))
                .asScalar()
            );

            this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, inv);
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
              Integer level = tryParseInt(p, levelStr).orElse(null);
              if (level == null) {
                this.show(p, inst.getArg(), AnimationType.SLIDE_UP);
                return;
              }

              meta.addEnchant(enchantment, level, true);
              item.setItemMeta(meta);

              p.sendMessage(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_ENCHANTMENT_ADDED)
                  .withPrefix()
                  .withVariable("enchantment", formatConstant(enchantment.getKey().getKey()))
                  .withVariable("level", level)
                  .asScalar()
              );

              this.show(p, inst.getArg(), AnimationType.SLIDE_UP);
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

          this.show(p, inst.getArg(), AnimationType.SLIDE_UP);
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

          this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, inv);
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

              this.show(p, inst.getArg(), AnimationType.SLIDE_UP);
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

              this.show(p, inst.getArg(), AnimationType.SLIDE_UP);
              return;
            }

            p.sendMessage(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_SELECT_POS)
                .withPrefix()
                .asScalar()
            );

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

              this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, inv);
              return false;
            }, closed, backButton);
          },

          closed
        );

        // Close the GUI when prompting for the chat message
        e.getGui().close();
      }
    });

    //////////////////////////////////// Unbreakability ////////////////////////////////////

    inst.fixedItem(33, i -> {
      boolean isDamageable = (meta instanceof Damageable && item.getType().getMaxDurability() > 0);
      int currDur = item.getType().getMaxDurability() - ((meta instanceof Damageable d) ? d.getDamage() : -1);
      int maxDur = item.getType().getMaxDurability();

      return new ItemStackBuilder(Material.ANVIL)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_DURABILITY_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_DURABILITY_LORE)
            .withVariable(
              "durability",
              cfg.get(
                meta.isUnbreakable() ?
                  ConfigKey.GUI_ITEMEDITOR_DURABILITY_UNBREAKABLE :
                  (isDamageable ? ConfigKey.GUI_ITEMEDITOR_DURABILITY_BREAKABLE : ConfigKey.GUI_ITEMEDITOR_DURABILITY_NON_BREAKABLE)
                )
                .withVariable("current_durability", currDur)
                .withVariable("max_durability", maxDur)
                .asScalar()
            )
        )
        .build();
    }, e -> {

      int maxDur = item.getType().getMaxDurability();

      // This item cannot take any damage
      if (!(meta instanceof Damageable d) || maxDur <= 0) {
        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_DURABILITY_NOT_BREAKABLE)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      // Decide on the step size, 16 steps should get you all the way down/up
      int stepSize = maxDur / 16;

      ClickType click = e.getManipulation().getClick();

      if (click.isLeftClick()) {
        // Set unbreakable
        if (click.isShiftClick()) {
          if (meta.isUnbreakable()) {
            p.sendMessage(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_DURABILITY_UNBREAKABLE_NOT_INACTIVE)
                .withPrefix()
                .asScalar()
            );
            return;
          }

          meta.setUnbreakable(true);
          item.setItemMeta(meta);

          // Redraw the display and the durability icon
          inst.redraw("13," + e.getManipulation().getTargetSlot());

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_DURABILITY_UNBREAKABLE_ACTIVE)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        // Increase durability

        // Apply the constrained damage
        int damage = Math.max(d.getDamage() - stepSize, 0);
        d.setDamage(damage);
        item.setItemMeta(meta);

        // Redraw the display and the durability icon
        inst.redraw("13," + e.getManipulation().getTargetSlot());

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_DURABILITY_CHANGED)
            .withPrefix()
            .withVariable("current_durability", maxDur - damage)
            .withVariable("max_durability", maxDur)
            .asScalar()
        );
        return;
      }

      if (click.isRightClick()) {
        // Remove unbreakability
        if (click.isShiftClick()) {
          if (!meta.isUnbreakable()) {
            p.sendMessage(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_DURABILITY_UNBREAKABLE_NOT_ACTIVE)
                .withPrefix()
                .asScalar()
            );
            return;
          }

          meta.setUnbreakable(false);
          item.setItemMeta(meta);

          // Redraw the display and the durability icon
          inst.redraw("13," + e.getManipulation().getTargetSlot());

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_DURABILITY_UNBREAKABLE_INACTIVE)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        // Decrease durability

        // Apply the constrained damage
        int damage = Math.min(d.getDamage() + stepSize, maxDur - 1);
        d.setDamage(damage);
        item.setItemMeta(meta);

        // Redraw the display and the durability icon
        inst.redraw("13," + e.getManipulation().getTargetSlot());

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_DURABILITY_CHANGED)
            .withPrefix()
            .withVariable("current_durability", maxDur - damage)
            .withVariable("max_durability", maxDur)
            .asScalar()
        );
      }
    });

    ////////////////////////////////////// Attributes //////////////////////////////////////

    // NOTE: Prepare for indentation hell, :^)

    inst.fixedItem(34, i -> (
      new ItemStackBuilder(Material.COMPARATOR)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_LORE))
        .build()
    ), e -> {

      ClickType click = e.getManipulation().getClick();

      if (click.isRightClick()) {
        Multimap<Attribute, AttributeModifier> attrs = meta.getAttributeModifiers();
        if (attrs == null) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_HAS_NONE)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        // Remove all available attributes
        if (click.isShiftClick()) {
          for (Map.Entry<Attribute, AttributeModifier> entry : attrs.entries())
            meta.removeAttributeModifier(entry.getKey(), entry.getValue());

          // Set and redraw the item
          item.setItemMeta(meta);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_CLEARED)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        // Remove a specific attribute
        openAttributeIndexChoice(attrs, true, inst, (target, inv) -> {

          // Remove the chosen attribute
          meta.removeAttributeModifier(target.a(), target.b());
          item.setItemMeta(meta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_REMOVED)
              .withPrefix()
              .withVariable("attribute", formatConstant(target.a().getKey().getKey()))
              .asScalar()
          );

          this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, inv);
          return false;
        }, closed, backButton);

        return;
      }

      if (click.isLeftClick()) {
        // Create a list of attributes to choose from
        Multimap<Attribute, AttributeModifier> attrs = ArrayListMultimap.create();
        for (Attribute attr : Attribute.values())
          attrs.put(attr, null);

        // Choose an attribute to add
        openAttributeIndexChoice(attrs, false, inst, (target, attrChoiceInv) -> {
          Attribute attr = target.a();

          // Prompt for the desired amount in the chat
          chatUtil.registerPrompt(
            viewer,
            cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_AMOUNT_PROMPT)
              .withPrefix()
              .asScalar(),

            // Amount entered
            amountStr -> {

              // Parse the amount from the string
              double amount;
              try {
                amount = Double.parseDouble(amountStr);
              } catch (NumberFormatException ex) {
                viewer.sendMessage(
                  cfg.get(ConfigKey.ERR_FLOATPARSE)
                    .withPrefix()
                    .withVariable("number", amountStr)
                    .asScalar()
                );

                this.show(p, inst.getArg(), AnimationType.SLIDE_UP);
                return;
              }

              // Create a list of all available slots
              List<Tuple<Object, ItemStack>> slotReprs = new ArrayList<>();
              for (EquipmentSlot slot : EquipmentSlot.values()) {
                slotReprs.add(new Tuple<>(
                  slot,
                  new ItemStackBuilder(Material.CHEST)
                    .withName(
                      cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_EQUIPMENT_NAME)
                        .withVariable("slot", formatConstant(slot.name()))
                    )
                    .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_EQUIPMENT_LORE))
                    .build()
                ));
              }

              // Invoke a new single choice gui for available slots
              singleChoiceGui.show(viewer, new SingleChoiceParam(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_EQUIPMENT_TITLE).asScalar(), slotReprs,

                // Slot selected
                (slot, slotChoiceInv) -> {

                  // Create a list of all available operations
                  List<Tuple<Object, ItemStack>> opReprs = new ArrayList<>();
                  for (AttributeModifier.Operation op : AttributeModifier.Operation.values()) {
                    opReprs.add(new Tuple<>(
                      op,
                      new ItemStackBuilder(Material.REDSTONE_TORCH)
                        .withName(
                          cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_OPERATION_NAME)
                            .withVariable("operation", formatConstant(op.name()))
                        )
                        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_OPERATION_LORE))
                        .build()
                    ));
                  }

                  // Invoke a new single choice gui for available operations
                  singleChoiceGui.show(viewer, new SingleChoiceParam(
                    cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_OPERATION_TITLE).asScalar(), opReprs,

                    // Operation selected
                    (op, opChoiceInv) -> {

                      meta.addAttributeModifier(target.a(), new AttributeModifier(
                        UUID.randomUUID(),
                        attr.name(),
                        amount,
                        (AttributeModifier.Operation) op,
                        (EquipmentSlot) slot
                      ));

                      item.setItemMeta(meta);

                      p.sendMessage(
                        cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_ADDED)
                          .withPrefix()
                          .withVariable("attribute", formatConstant(attr.getKey().getKey()))
                          .asScalar()
                      );

                      this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, opChoiceInv);
                      return false;
                    },

                    closed, backButton
                  ), AnimationType.SLIDE_LEFT, slotChoiceInv);

                  return false;
                },

                closed, backButton
              ), AnimationType.SLIDE_UP);

            },

            closed
          );

          // Close the GUI when prompting for the chat message
          return true;
        }, closed, backButton);
      }
    });

    ///////////////////////////////////// Skull Owner /////////////////////////////////////

    inst.fixedItem(27, i -> (
      new ItemStackBuilder(Material.SKELETON_SKULL)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_SKULLOWNER_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_SKULLOWNER_LORE))
        .build()
    ), e -> {
      // Not an item which will have skull meta
      if (!(meta instanceof SkullMeta skullMeta)) {
        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_SKULLOWNER_NO_SKULL)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      // Prompt for the desired head owner in the chat
      chatUtil.registerPrompt(
        viewer,
        cfg.get(ConfigKey.GUI_ITEMEDITOR_SKULLOWNER_PROMPT)
          .withPrefix()
          .asScalar(),

        // Owner entered
        ownerStr -> {
          // Load the corresponding textures
          PlayerTextureModel ownerTextures = textures.getTextures(ownerStr, false).orElse(null);
          if (ownerTextures == null) {
            p.sendMessage(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_SKULLOWNER_NOT_LOADABLE)
                .withPrefix()
                .withVariable("owner", ownerStr)
                .asScalar()
            );

            this.show(p, inst.getArg(), AnimationType.SLIDE_UP);
            return;
          }

          // Overwrite the GameProfile of the skull
          try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, ownerTextures.toProfile());
          } catch (Exception ex) {
            logger.logError(ex);
          }

          item.setItemMeta(skullMeta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_SKULLOWNER_CHANGED)
              .withPrefix()
              .withVariable("owner", ownerTextures.getName())
              .asScalar()
          );

          this.show(p, inst.getArg(), AnimationType.SLIDE_UP);
        },

        closed
      );

      // Close the GUI when prompting for the chat message
      e.getGui().close();
    });

    ///////////////////////////////////// Leather Color /////////////////////////////////////

    inst.fixedItem(35, i -> (
      new ItemStackBuilder(Material.LEATHER)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_LORE))
        .build()
    ), e -> {
      // Not an item which will have leather armor meta
      if (!(meta instanceof LeatherArmorMeta armorMeta)) {
        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_NO_LEATHER)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      ClickType click = e.getManipulation().getClick();

      // Set to an RGB value
      if (click.isRightClick()) {

        // Prompt for the desired RGB color value in the chat
        chatUtil.registerPrompt(
          viewer,
          cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_PROMPT)
            .withPrefix()
            .asScalar(),

          // Color entered
          colorStr -> {
            String[] colorData = colorStr.split(" ");

            // Parts missing or excess
            if (colorData.length != 3) {
              p.sendMessage(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_INVALID_FORMAT)
                  .withPrefix()
                  .withVariable("input", colorStr)
                  .asScalar()
              );

              this.show(p, inst.getArg(), AnimationType.SLIDE_UP);
              return;
            }

            Integer r = tryParseInt(p, colorData[0]).orElse(null);
            Integer g = tryParseInt(p, colorData[1]).orElse(null);
            Integer b = tryParseInt(p, colorData[2]).orElse(null);

            if (
              // Not a number
              r == null || g == null || b == null ||

              // Out of range
              r < 0 || g < 0 || b < 0 ||
              r > 255 || g > 255 || b > 255
            ) {
              p.sendMessage(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_INVALID_FORMAT)
                  .withPrefix()
                  .withVariable("input", colorStr)
                  .asScalar()
              );

              this.show(p, inst.getArg(), AnimationType.SLIDE_UP);
              return;
            }

            armorMeta.setColor(Color.fromRGB(r, g, b));
            item.setItemMeta(armorMeta);

            p.sendMessage(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_CHANGED)
                .withPrefix()
                .withVariable("color", colorStr)
                .asScalar()
            );

            this.show(p, inst.getArg(), AnimationType.SLIDE_UP);
          },

          closed
        );

        // Close the GUI when prompting for the chat message
        e.getGui().close();
        return;
      }

      // Set to a predefined value
      if (click.isLeftClick()) {

        List<Tuple<String, Color>> colors = new ArrayList<>();

        // Get all available colors from the class's list of constant fields
        try {
          List<Field> constants = Arrays.stream(Color.class.getDeclaredFields())
            .filter(field -> field.getType().equals(Color.class) && Modifier.isStatic(field.getModifiers()))
            .toList();

          for (Field constant : constants)
            colors.add(new Tuple<>(constant.getName(), (Color) constant.get(null)));
        } catch (Exception ex) {
          logger.logError(ex);
        }

        // Create a list of all available slots
        List<Tuple<Object, ItemStack>> slotReprs = new ArrayList<>();
        for (Tuple<String, Color> color : colors) {
          slotReprs.add(new Tuple<>(
            color,
            new ItemStackBuilder(item.getType())
              .withColor(color.b())
              .withName(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_LEATHERCOLOR_NAME)
                  .withVariable("color", formatConstant(color.a()))
              )
              .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_LEATHERCOLOR_LORE))
              .build()
          ));
        }

        // Invoke a new single choice gui for available colors
        inst.switchTo(AnimationType.SLIDE_LEFT, singleChoiceGui, new SingleChoiceParam(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_LEATHERCOLOR_TITLE).asScalar(), slotReprs,

          // Color selected
          (color, inv) -> {
            @SuppressWarnings("unchecked")
            Tuple<String, Color> colorData = (Tuple<String, Color>) color;

            armorMeta.setColor(colorData.b());
            item.setItemMeta(armorMeta);

            p.sendMessage(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_CHANGED)
                .withPrefix()
                .withVariable("color", formatConstant(colorData.a()))
                .asScalar()
            );

            this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, inv);
            return false;
          },
          closed, backButton
        ));
      }
    });

    return true;
  }

      /**
       * Tries to parse an integer from a string value and notifies the player on malformed input.
   * @param p Target player
   * @param input String to parse
   * @return Optional number, empty on malformed input
   */
  private Optional<Integer> tryParseInt(Player p, String input) {
    try {
      return Optional.of(Integer.parseInt(input));
    } catch (Exception ex) {
      p.sendMessage(
        cfg.get(ConfigKey.ERR_INTPARSE)
          .withVariable("number", input)
          .asScalar()
      );
      return Optional.empty();
    }
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
    GuiInstance<Triple<ItemStack, @Nullable Consumer<ItemStack>, @Nullable Consumer<Inventory>>> inst,
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

  /**
   * Open a new attribute choice GUI which presents the user with an item for each
   * attribute and then results in the ref of the attribute chosen
   * @param attrs Available attributes
   * @param areExisting Whether these attributes are already existing, or they're a
   *                    list of all available attributes (attributemodifier is null)
   * @param inst Gui instance to animate away from
   * @param chosen Chosen callback
   * @param closed Closed callback
   * @param backButton Back button callback
   */
  @SuppressWarnings("unchecked")
  private void openAttributeIndexChoice(
    Multimap<Attribute, AttributeModifier> attrs,
    boolean areExisting,
    GuiInstance<Triple<ItemStack, @Nullable Consumer<ItemStack>, @Nullable Consumer<Inventory>>> inst,
    BiFunction<Tuple<Attribute, AttributeModifier>, Inventory, Boolean> chosen,
    Runnable closed,
    Consumer<Inventory> backButton
  ) {
    // Create representitive items for each attribute
    List<Tuple<Object, ItemStack>> representitives = new ArrayList<>();
    for (Map.Entry<Attribute, AttributeModifier> entry : attrs.entries()) {
      AttributeModifier mod = entry.getValue();

      ConfigValue lore = cfg.get(
          areExisting ?
            ConfigKey.GUI_ITEMEDITOR_CHOICE_ATTR_EXISTING_LORE :
            ConfigKey.GUI_ITEMEDITOR_CHOICE_ATTR_NEW_LORE
        );

      if (areExisting) {
        lore.withVariable("name", mod.getName())
        .withVariable("amount", mod.getAmount())
        .withVariable("operation", formatConstant(mod.getOperation().name()))
        .withVariable("slot", mod.getSlot() == null ? "/" : formatConstant(mod.getSlot().name()));
      }

      representitives.add(new Tuple<>(
        new Tuple<>(entry.getKey(), entry.getValue()),
        new ItemStackBuilder(Material.COMPARATOR)
          .withName(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_ATTR_NAME)
              .withVariable("attribute", formatConstant(entry.getKey().getKey().getKey()))
          )
          .withLore(lore)
          .build()
      ));
    }

    // Invoke a new single choice gui for available attributes
    inst.switchTo(AnimationType.SLIDE_LEFT, singleChoiceGui, new SingleChoiceParam(
      cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_ATTR_TITLE).asScalar(),
      representitives,
      (m, inv) -> chosen.apply((Tuple<Attribute, AttributeModifier>) m, inv),
      closed, backButton
    ));
  }

  /**
   * Formats a constant to a human readable string
   * @param constant Constant to format
   * @return Formatted string
   */
  private String formatConstant(String constant) {
    return WordUtils.capitalizeFully(constant.replace("_", " ").replace(".", " "));
  }
}
