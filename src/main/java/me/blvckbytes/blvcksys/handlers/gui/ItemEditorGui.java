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
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Edit all possible properties of an itemstack while always
  having a preview available.
  Args: Item to edit, item change callback, back button callback
*/
@AutoConstruct
public class ItemEditorGui extends AGui<Triple<ItemStack, @Nullable Consumer<ItemStack>, @Nullable Consumer<GuiInstance<?>>>> {

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
    super(6, "", i -> (
      cfg.get(ConfigKey.GUI_ITEMEDITOR_TITLE)
        .withVariable("item_type", i.getArg().a().getType().name())
    ), plugin, cfg, textures);

    this.singleChoiceGui = singleChoiceGui;
    this.logger = logger;
    this.chatUtil = chatUtil;
  }

  @Override
  protected boolean closed(GuiInstance<Triple<ItemStack, @Nullable Consumer<ItemStack>, @Nullable Consumer<GuiInstance<?>>>> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Triple<ItemStack, @Nullable Consumer<ItemStack>, @Nullable Consumer<GuiInstance<?>>>> inst) {
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
    Consumer<GuiInstance<?>> back = inst.getArg().c();
    if (back != null) {
      inst.addBack(45, e -> back.accept(inst));
    }

    ///////////////////////////////////// Preview //////////////////////////////////////

    inst.fixedItem("12,14", () -> new ItemStackBuilder(Material.PURPLE_STAINED_GLASS_PANE).build(), null);
    inst.fixedItem(13, () -> item, null);

    // Fire the item update callback whenever the preview slot changes
    inst.onRedrawing(13, () -> {
      Consumer<ItemStack> cb = inst.getArg().b();
      if (cb != null)
        cb.accept(item);
    });

    ///////////////////////////////// Increase Amount //////////////////////////////////

    inst.fixedItem(10, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.ARROW_UP.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_AMOUNT_INCREASE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_AMOUNT_INCREASE_LORE))
        .build()
    ), e -> {
      ClickType click = e.getClick();
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

    inst.fixedItem(16, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.ARROW_DOWN.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_AMOUNT_DECREASE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_AMOUNT_DECREASE_LORE))
        .build()
    ), e -> {
      ClickType click = e.getClick();
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

    ///////////////////////////////////// Material /////////////////////////////////////

    inst.fixedItem(28, () -> (
      new ItemStackBuilder(Material.CHEST)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_MATERIAL_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_MATERIAL_LORE))
        .build()
    ), e -> {
      new UserInputChain(inst, values -> {
        Material mat = (Material) values.get("material");
        item.setType(mat);

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_MATERIAL_CHANGED)
            .withPrefix()
            .withVariable("material", formatConstant(mat.name()))
            .asScalar()
        );
      }, singleChoiceGui, chatUtil)
        .withChoice(
          "material",
          cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_MATERIAL_TITLE),
          this::buildMaterialRepresentitives,
          null
        )
        .start();
    });

    /////////////////////////////////// Item Flags ///////////////////////////////////

    inst.fixedItem(29, () -> (
      new ItemStackBuilder(Material.NAME_TAG)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_FLAGS_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_FLAGS_LORE))
        .build()
    ), e -> {
      new UserInputChain(inst, values -> {
        ItemFlag flag = (ItemFlag) values.get("flag");

        // Toggle the flag
        boolean has = meta.hasItemFlag(flag);
        if (has)
          meta.removeItemFlags(flag);
        else
          meta.addItemFlags(flag);

        item.setItemMeta(meta);

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
      }, singleChoiceGui, chatUtil)
        .withChoice(
          "flag",
          cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_TITLE),
          () -> buildItemFlagRepresentitives(meta::hasItemFlag),
          null
        )
        .start();
    });

    //////////////////////////////////// Enchantments ////////////////////////////////////

    inst.fixedItem(30, () -> (
      new ItemStackBuilder(Material.ENCHANTED_BOOK)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_ENCHANTMENTS_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_ENCHANTMENTS_LORE))
        .build()
    ), e -> {
      new UserInputChain(inst, values -> {
        Enchantment enchantment = (Enchantment) values.get("enchantment");
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
          return;
        }

        // Add the enchantment
        int level = (int) values.get("level");
        meta.addEnchant(enchantment, level, true);
        item.setItemMeta(meta);

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_ENCHANTMENT_ADDED)
            .withPrefix()
            .withVariable("enchantment", formatConstant(enchantment.getKey().getKey()))
            .withVariable("level", level)
            .asScalar()
        );
      }, singleChoiceGui, chatUtil)
        .withChoice(
          "enchantment",
          cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_TITLE),
          () -> buildEnchantmentRepresentitives(
            ench -> ench.canEnchantItem(item),
            meta::hasEnchant,
            meta::getEnchantLevel
          ),
          null
        )
        .withPrompt(
          "level",
          values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_ENCHANTMENT_LEVEL_PROMPT)
            .withVariable("enchantment", formatConstant(((Enchantment) values.get("enchantment")).getKey().getKey()))
            .withPrefix(),
          Integer::parseInt,
          input -> cfg.get(ConfigKey.ERR_INTPARSE).withVariable("number", input),
          values -> meta.hasEnchant((Enchantment) values.get("enchantment"))
        )
        .start();
    });

    //////////////////////////////////// Displayname ////////////////////////////////////

    inst.fixedItem(31, () -> (
      new ItemStackBuilder(Material.PAPER)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_DISPLAYNAME_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_DISPLAYNAME_LORE))
        .build()
    ), e -> {
      new UserInputChain(inst, values -> {
        String name = (String) values.get("name");

        boolean reset = name.equalsIgnoreCase("null");
        name = ChatColor.translateAlternateColorCodes('&', name);

        meta.setDisplayName(reset ? null : name);
        item.setItemMeta(meta);

        p.sendMessage(
          cfg.get(reset ? ConfigKey.GUI_ITEMEDITOR_DISPLAYNAME_RESET : ConfigKey.GUI_ITEMEDITOR_DISPLAYNAME_SET)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
      }, singleChoiceGui, chatUtil)
        .withPrompt(
          "name",
          values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_DISPLAYNAME_PROMPT)
            .withPrefix(),
          s -> s, null, null
        )
        .start();
    });

    //////////////////////////////////// Lore Lines ////////////////////////////////////

    inst.fixedItem(32, () -> (
      new ItemStackBuilder(Material.OAK_SIGN)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_LORE))
        .build()
    ), e -> {
      ClickType click = e.getClick();

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

        new UserInputChain(inst, values -> {
          int index = (int) values.get("index");

          String content = lines.remove(index);
          meta.setLore(lines);
          item.setItemMeta(meta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_LINE_REMOVED)
              .withPrefix()
              .withVariable("line_number", index + 1)
              .withVariable("line_content", content)
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "index",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_LORE_TITLE),
            () -> buildLoreRepresentitives(lines),
            null
          )
          .start();

        return;
      }

      // Add a new line
      if (click.isLeftClick()) {

        List<String> lines = meta.getLore() == null ? new ArrayList<>() : meta.getLore();

        new UserInputChain(inst, values -> {
          String line = ChatColor.translateAlternateColorCodes('&', (String) values.get("line"));

          // Translate null to an empty line
          if (line.equals("null"))
            line = " ";

          // Insert after index
          if (values.containsKey("index")) {
            int index = (int) values.get("index");
            lines.add(index, line);
          }

          // Push back
          else
            lines.add(line);

          meta.setLore(lines);
          item.setItemMeta(meta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_LINE_ADDED)
              .withPrefix()
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withPrompt(
            "line",
            values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_PROMPT)
              .withPrefix(),
            s -> s, null, null
          )
          .withChoice(
            "index",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_LORE_TITLE),
            () -> buildLoreRepresentitives(lines),
            // Shift means push back, no index required
            // Also, if there are no lines yet, just push back too
            values -> click.isShiftClick() || lines.size() == 0
          )
          .start();

      }
    });

    //////////////////////////////////// Unbreakability ////////////////////////////////////

    inst.fixedItem(33, () -> {
      boolean isDamageable = (meta instanceof Damageable && item.getType().getMaxDurability() > 0);
      int currDur = item.getType().getMaxDurability() - ((meta instanceof Damageable d) ? d.getDamage() : -1);
      int maxDur = item.getType().getMaxDurability();

      return new ItemStackBuilder(isDamageable ? Material.ANVIL : Material.BARRIER)
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
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_NOT_APPLICABLE_LORE), !isDamageable)
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

      ClickType click = e.getClick();

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
          inst.redraw("13," + e.getTargetSlot());

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
        inst.redraw("13," + e.getTargetSlot());

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
          inst.redraw("13," + e.getTargetSlot());

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
        inst.redraw("13," + e.getTargetSlot());

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

    inst.fixedItem(34, () -> (
      new ItemStackBuilder(Material.COMPARATOR)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_LORE))
        .build()
    ), e -> {

      ClickType click = e.getClick();

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

        new UserInputChain(inst, values -> {
          @SuppressWarnings("unchecked")
          Tuple<Attribute, AttributeModifier> attr = (Tuple<Attribute, AttributeModifier>) values.get("attribute");

          // Remove the chosen attribute
          meta.removeAttributeModifier(attr.a(), attr.b());
          item.setItemMeta(meta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_REMOVED)
              .withPrefix()
              .withVariable("attribute", formatConstant(attr.a().getKey().getKey()))
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "attribute",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_ATTR_TITLE),
            () -> buildAttributeRepresentitives(attrs, true),
            null
          )
          .start();

        return;
      }

      if (click.isLeftClick()) {

        new UserInputChain(inst, values -> {
          @SuppressWarnings("unchecked")
          Tuple<Attribute, AttributeModifier> attr = (Tuple<Attribute, AttributeModifier>) values.get("attribute");
          double amount = (double) values.get("amount");
          EquipmentSlot slot = (EquipmentSlot) values.get("slot");
          AttributeModifier.Operation op = (AttributeModifier.Operation) values.get("operation");

          meta.addAttributeModifier(attr.a(), new AttributeModifier(
            UUID.randomUUID(),
            attr.a().name(),
            amount, op, slot
          ));

          item.setItemMeta(meta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_ADDED)
            .withPrefix()
            .withVariable("attribute", formatConstant(attr.a().name()))
            .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "attribute",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_ATTR_TITLE),
            () -> (
              buildAttributeRepresentitives(
                // Create a "fake" multimap which contains an entry for every attribute with a null modifier
                Arrays.stream(Attribute.values())
                  .collect(ArrayListMultimap::create, (m, v) -> m.put(v, null), ArrayListMultimap::putAll),
                false
              )
            ),
            null
          )
          .withPrompt(
            "amount",
            values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_ATTRIBUTES_AMOUNT_PROMPT).withPrefix(),
            Double::parseDouble,
            inp -> (
              cfg.get(ConfigKey.ERR_FLOATPARSE)
                .withPrefix()
                .withVariable("number", inp)
            ),
            null
          )
          .withChoice(
            "slot",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_EQUIPMENT_TITLE),
            this::buildSlotRepresentitives,
            null
          )
          .withChoice(
            "operation",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_OPERATION_TITLE),
            this::buildOperationRepresentitives,
            null
          )
          .start();

      }
    });

    ///////////////////////////////////// Skull Owner /////////////////////////////////////

    inst.fixedItem(39, () -> {
      boolean applicable = meta instanceof SkullMeta;
      return new ItemStackBuilder(applicable ? Material.SKELETON_SKULL : Material.BARRIER)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_SKULLOWNER_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_SKULLOWNER_LORE))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_NOT_APPLICABLE_LORE), !applicable)
        .build();
    }, e -> {
      // Not an item which will have skull meta
      if (!(meta instanceof SkullMeta skullMeta))
        return;

      new UserInputChain(inst, values -> {
        String owner = (String) values.get("owner");

        // Load the corresponding textures
        PlayerTextureModel ownerTextures = textures.getTextures(owner, false).orElse(null);
        if (ownerTextures == null) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_SKULLOWNER_NOT_LOADABLE)
              .withPrefix()
              .withVariable("owner", owner)
              .asScalar()
          );

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
      }, singleChoiceGui, chatUtil)
        .withPrompt(
          "owner",
          values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_SKULLOWNER_PROMPT)
            .withPrefix(),
          s -> s, null, null
        )
        .start();
    });

    ///////////////////////////////////// Leather Color /////////////////////////////////////

    inst.fixedItem(40, () -> {
      boolean applicable = meta instanceof LeatherArmorMeta;
      return new ItemStackBuilder(applicable ? Material.LEATHER : Material.BARRIER)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_LORE))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_NOT_APPLICABLE_LORE), !applicable)
        .build();
    }, e -> {
      // Not an item which will have leather armor meta
      if (!(meta instanceof LeatherArmorMeta armorMeta))
        return;

      ClickType click = e.getClick();

      // Set to an RGB value
      if (click.isRightClick()) {

        new UserInputChain(inst, values -> {
          @SuppressWarnings("unchecked")
          Tuple<Color, String> color = (Tuple<Color, String>) values.get("color");
          armorMeta.setColor((color.a()) );
          item.setItemMeta(armorMeta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_CHANGED)
              .withPrefix()
              .withVariable("color", color.b())
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withPrompt(
            "color",
            values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_COLOR_PROMPT)
              .withPrefix(),
            s -> {
              String[] data = s.split(" ");
              return new Tuple<>(
                Color.fromRGB(
                  Integer.parseInt(data[0]),
                  Integer.parseInt(data[1]),
                  Integer.parseInt(data[2])
                ),
                s
              );
            },
            input -> (
              cfg.get(ConfigKey.GUI_ITEMEDITOR_COLOR_INVALID_FORMAT)
                .withPrefix()
                .withVariable("input", input)
            ),
            null
          )
          .start();

        return;
      }

      // Set to a predefined value
      if (click.isLeftClick()) {

        new UserInputChain(inst, values -> {
          @SuppressWarnings("unchecked")
          Tuple<String, Color> colorData = (Tuple<String, Color>) values.get("color");

          armorMeta.setColor(colorData.b());
          item.setItemMeta(armorMeta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_LEATHERCOLOR_CHANGED)
              .withPrefix()
              .withVariable("color", formatConstant(colorData.a()))
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "color",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_LEATHERCOLOR_TITLE),
            () -> generateColorReprs(
              c -> item.getType(),
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_LEATHERCOLOR_NAME),
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_LEATHERCOLOR_LORE)
            ),
            null
          )
          .start();

      }
    });

    ///////////////////////////////////// Potion Effects /////////////////////////////////////

    inst.fixedItem(41, () -> {
      boolean applicable = meta instanceof PotionMeta;
      return new ItemStackBuilder(applicable ? Material.BREWING_STAND : Material.BARRIER)
        .withName(cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_NAME))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_LORE))
        .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_NOT_APPLICABLE_LORE), !applicable)
        .build();
    }, e -> {
      // Not an item which will have potion meta
      if (!(meta instanceof PotionMeta potionMeta))
        return;

      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      // Change main effect type
      if (key == 1) {
        new UserInputChain(inst, values -> {
          PotionType type = (PotionType) values.get("type");
          PotionData data = potionMeta.getBasePotionData();
          potionMeta.setBasePotionData(new PotionData(type, type.isExtendable() && data.isExtended(), type.isUpgradeable() && data.isUpgraded()));
          item.setItemMeta(potionMeta);
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "type",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_POTION_TYPE_TITLE),
            () -> generatePotionTypeReprs(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_POTION_TYPE_NAME),
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_POTION_TYPE_LORE)
            ),
            null
          )
          .start();

        return;
      }

      // Upgrade main effect duration
      if (key == 2) {
        PotionData data = potionMeta.getBasePotionData();

        if (!data.getType().isExtendable()) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_NOT_EXTENDABLE)
              .withPrefixes()
              .asScalar()
          );
          return;
        }

        potionMeta.setBasePotionData(new PotionData(data.getType(), true, false));
        item.setItemMeta(potionMeta);
        inst.redraw("13");

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_DURATION_EXTENDED)
            .withPrefixes()
            .asScalar()
        );

        return;
      }

      // Upgrade main effect level
      if (key == 3) {
        PotionData data = potionMeta.getBasePotionData();

        if (!data.getType().isUpgradeable()) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_NOT_UPGRADABLE)
              .withPrefixes()
              .asScalar()
          );
          return;
        }

        potionMeta.setBasePotionData(new PotionData(data.getType(), false, true));
        item.setItemMeta(potionMeta);
        inst.redraw("13");

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_LEVEL_UPGRADED)
            .withPrefixes()
            .asScalar()
        );

        return;
      }

      // Add secondary effect
      if (key == 4) {
        new UserInputChain(inst, values -> {
          PotionEffectType type = ((PotionEffect) values.get("type")).getType();
          int duration = (int) values.get("duration");
          int amplifier = (int) values.get("amplifier");

          // Duration is in ticks
          duration = Math.max(1, duration) * 20;
          // Amplifiers start at zero
          amplifier = Math.max(0, amplifier - 1);

          potionMeta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
          item.setItemMeta(meta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_ADDED)
              .withPrefix()
              .withVariable("effect", formatConstant(type.getName()))
              .withVariable("duration", duration / 20)
              .withVariable("level", amplifier + 1)
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "type",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_POTION_EFFECT_TITLE),
            () -> buildPotionEffectRepresentitives(null),
            null
          )
          .withPrompt(
            "duration",
            values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_DURATION_PROMPT)
              .withPrefix(),
            Integer::parseInt,
            input -> cfg.get(ConfigKey.ERR_INTPARSE).withVariable("number", input),
            null
          )
          .withPrompt(
            "amplifier",
            values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_AMPLIFIER_PROMPT)
              .withPrefix(),
            Integer::parseInt,
            input -> cfg.get(ConfigKey.ERR_INTPARSE).withVariable("number", input),
            null
          )
          .start();
      }

      // Remove a secondary effect
      if (key == 5) {
        List<PotionEffect> secondaries = potionMeta.getCustomEffects();

        if (secondaries.size() == 0) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_NO_SECONDARY)
              .withPrefixes()
              .asScalar()
          );
          return;
        }

        new UserInputChain(inst, values -> {
          PotionEffect effect = (PotionEffect) values.get("effect");

          potionMeta.removeCustomEffect(effect.getType());
          item.setItemMeta(meta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_REMOVED)
              .withPrefix()
              .withVariable("effect", formatConstant(effect.getType().getName()))
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "effect",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_POTION_EFFECT_TITLE),
            () -> buildPotionEffectRepresentitives(potionMeta.getCustomEffects()),
            null
          )
          .start();
      }

      // Set to a predefined value
      if (key == 6) {
        new UserInputChain(inst, values -> {
          @SuppressWarnings("unchecked")
          Tuple<String, Color> colorData = (Tuple<String, Color>) values.get("color");

          potionMeta.setColor(colorData.b());
          item.setItemMeta(potionMeta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONCOLOR_CHANGED)
              .withPrefix()
              .withVariable("color", formatConstant(colorData.a()))
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "color",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_POTIONCOLOR_TITLE),
            () -> generateColorReprs(
              c -> item.getType(),
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_POTIONCOLOR_NAME),
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_POTIONCOLOR_LORE)
            ),
            null
          )
          .start();

      }

      // Set to a custom RGB color
      if (key == 7) {
        new UserInputChain(inst, values -> {
          @SuppressWarnings("unchecked")
          Tuple<Color, String> color = (Tuple<Color, String>) values.get("color");
          potionMeta.setColor((color.a()) );
          item.setItemMeta(potionMeta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONCOLOR_CHANGED)
              .withPrefix()
              .withVariable("color", color.b())
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withPrompt(
            "color",
            values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_COLOR_PROMPT)
              .withPrefix(),
            s -> {
              String[] data = s.split(" ");
              return new Tuple<>(
                Color.fromRGB(
                  Integer.parseInt(data[0]),
                  Integer.parseInt(data[1]),
                  Integer.parseInt(data[2])
                ),
                s
              );
            },
            input -> (
              cfg.get(ConfigKey.GUI_ITEMEDITOR_COLOR_INVALID_FORMAT)
                .withPrefix()
                .withVariable("input", input)
            ),
            null
          )
          .start();

        return;
      }

      // Reset color
      if (key == 8) {
        if (potionMeta.getColor() == null) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONCOLOR_NONE)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        potionMeta.setColor(null);
        item.setItemMeta(potionMeta);
        inst.redraw("13");

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONCOLOR_RESET)
            .withPrefix()
            .asScalar()
        );
      }

    });

    return true;
  }

  /**
   * Build a list of representitives for all available potion effects
   * @param effects List of existing effects, leave null to build all available effects
   */
  private List<Tuple<Object, ItemStack>> buildPotionEffectRepresentitives(@Nullable List<PotionEffect> effects) {
    // Create representitive items for each effect
    List<Tuple<Object, ItemStack>> representitives = new ArrayList<>();

    // When no effects have been provided, use all enum values
    if (effects == null) {
      effects = Arrays.stream(PotionEffectType.values())
        .map(type -> new PotionEffect(type, 20 * 60, 0))
        .toList();
    }

    for (PotionEffect effect : effects) {
      representitives.add(new Tuple<>(
        effect,
        new ItemStackBuilder(Material.POTION)
          .withName(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_POTION_EFFECT_NAME)
              .withVariable("effect", formatConstant(effect.getType().getName()))
          )
          .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_POTION_EFFECT_LORE))
          .withCustomEffects(() -> List.of(effect), true)
          .build()
      ));
    }

    return representitives;
  }

  /**
   * Build a list of representitives for all available lines within the list
   * @param lore List of lore lines
   */
  private List<Tuple<Object, ItemStack>> buildLoreRepresentitives(List<String> lore) {
    // Create representitive items for each line
    List<Tuple<Object, ItemStack>> representitives = new ArrayList<>();
    for (int i = 0; i < lore.size(); i++) {
      String line = lore.get(i);
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
    return representitives;
  }

  /**
   * Build a list of representitives for all available materials
   * @param isActive Function used to determine whether a flag is marked as active
   */
  private List<Tuple<Object, ItemStack>> buildItemFlagRepresentitives(Function<ItemFlag, Boolean> isActive) {
    // Representitive items for each flag
    return Arrays.stream(ItemFlag.values())
      .map(f -> (
          new Tuple<>((Object) f, (
            new ItemStackBuilder(Material.NAME_TAG)
              .withName(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_NAME)
                  .withVariable("flag", formatConstant(f.name()))
              )
              .withLore(
                cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_LORE)
                  .withVariable(
                    "state",
                    cfg.get(
                      isActive.apply(f) ?
                        ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_ACTIVE :
                        ConfigKey.GUI_ITEMEDITOR_CHOICE_FLAG_INACTIVE
                      )
                      .asScalar()
                  )
              )
              .build()
          ))
        )
      ).toList();
  }

  /**
   * Build a list of representitives for all available materials
   */
  public List<Tuple<Object, ItemStack>> buildMaterialRepresentitives() {
    // Representitive items for each material
    return Arrays.stream(Material.values())
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
  }

  /**
   * Build a list of representitives for all available
   * {@link org.bukkit.attribute.AttributeModifier.Operation} enum entries
   */
  private List<Tuple<Object, ItemStack>> buildOperationRepresentitives() {
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
    return opReprs;
  }

  /**
   * Build a list of representitives for all available {@link EquipmentSlot} enum entries
   */
  private List<Tuple<Object, ItemStack>> buildSlotRepresentitives() {
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
    return slotReprs;
  }

  /**
   * Maps enchantments to representitive icon materials
   * @param ench Enchantment to map
   * @return Icon material to display
   */
  private Material enchantmentToMaterial(Enchantment ench) {
    if (ench == Enchantment.PROTECTION_ENVIRONMENTAL)
      return Material.DIAMOND_CHESTPLATE;

    if (ench == Enchantment.PROTECTION_FIRE)
      return Material.GOLDEN_LEGGINGS;

    if (ench == Enchantment.PROTECTION_EXPLOSIONS)
      return Material.TNT;

    if (ench == Enchantment.PROTECTION_FALL)
      return Material.FEATHER;

    if (ench == Enchantment.PROTECTION_PROJECTILE)
      return Material.ARROW;

    if (ench == Enchantment.BINDING_CURSE)
      return Material.CHAIN;

    if (ench == Enchantment.OXYGEN)
      return Material.GLASS_BOTTLE;

    if (ench == Enchantment.WATER_WORKER)
      return Material.WATER_BUCKET;

    if (ench == Enchantment.THORNS)
      return Material.POPPY;

    if (ench == Enchantment.DEPTH_STRIDER)
      return Material.DIAMOND_BOOTS;

    if (ench == Enchantment.FROST_WALKER)
      return Material.PACKED_ICE;

    if (ench == Enchantment.DAMAGE_ALL)
      return Material.DIAMOND_SWORD;

    if (ench == Enchantment.DAMAGE_UNDEAD)
      return Material.ZOMBIE_HEAD;

    if (ench == Enchantment.DAMAGE_ARTHROPODS)
      return Material.WOODEN_SWORD;

    if (ench == Enchantment.FIRE_ASPECT)
      return Material.FLINT_AND_STEEL;

    if (ench == Enchantment.KNOCKBACK)
      return Material.STICK;

    if (ench == Enchantment.LOOT_BONUS_MOBS)
      return Material.GUNPOWDER;

    if (ench == Enchantment.LOOT_BONUS_BLOCKS)
      return Material.DIAMOND;

    if (ench == Enchantment.SWEEPING_EDGE)
      return Material.LEAD;

    if (ench == Enchantment.DIG_SPEED)
      return Material.IRON_PICKAXE;

    if (ench == Enchantment.SILK_TOUCH)
      return Material.YELLOW_STAINED_GLASS;

    if (ench == Enchantment.DURABILITY)
      return Material.ANVIL;

    if (ench == Enchantment.ARROW_DAMAGE)
      return Material.BOW;

    if (ench == Enchantment.ARROW_FIRE)
      return Material.CANDLE;

    if (ench == Enchantment.ARROW_INFINITE)
      return Material.ARROW;

    if (ench == Enchantment.ARROW_KNOCKBACK)
      return Material.STICK;

    if (ench == Enchantment.LURE)
      return Material.FISHING_ROD;

    if (ench == Enchantment.LUCK)
      return Material.PUFFERFISH;

    if (ench == Enchantment.LOYALTY)
      return Material.TRIDENT;

    if (ench == Enchantment.IMPALING)
      return Material.TIPPED_ARROW;

    if (ench == Enchantment.RIPTIDE)
      return Material.ENDER_PEARL;

    if (ench == Enchantment.MULTISHOT)
      return Material.FIREWORK_ROCKET;

    if (ench == Enchantment.CHANNELING)
      return Material.IRON_BARS;

    if (ench == Enchantment.QUICK_CHARGE)
      return Material.CROSSBOW;

    if (ench == Enchantment.PIERCING)
      return Material.IRON_BLOCK;

    if (ench == Enchantment.MENDING)
      return Material.EXPERIENCE_BOTTLE;

    if (ench == Enchantment.VANISHING_CURSE)
      return Material.BUCKET;

    if (ench == Enchantment.SOUL_SPEED)
      return Material.SOUL_SAND;

    return Material.BOOK;
  }

  /**
   * Build a list of representitives for all available {@link Enchantment} constants
   * @param isNative Function used to check whether this enchantment is native to the target item
   * @param isActive Function used to check whether this enchantment is active on the target item
   * @param activeLevel Function used to get the currently active level of this enchantment on the target item
   */
  private List<Tuple<Object, ItemStack>> buildEnchantmentRepresentitives(
    Function<Enchantment, Boolean> isNative,
    Function<Enchantment, Boolean> isActive,
    Function<Enchantment, Integer> activeLevel
  ) {
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
    return enchantments.stream()
      // Sort by relevance
      .sorted(Comparator.comparing(isNative, Comparator.reverseOrder()))
      .map(ench -> {
          boolean has = isActive.apply(ench);
          int level = -1;

          if (has)
            level = activeLevel.apply(ench);

          return new Tuple<>((Object) ench, (
            new ItemStackBuilder(has ? Material.ENCHANTED_BOOK : enchantmentToMaterial(ench))
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
  }

  /**
   * Build a list of representitives for all attributes contained in the multimap
   * @param attrs Attributes to display
   * @param areExisting Whether there are modifiers existing, if false, modifiers are ignored
   */
  private List<Tuple<Object, ItemStack>> buildAttributeRepresentitives(Multimap<Attribute, AttributeModifier> attrs, boolean areExisting) {
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

    return representitives;
  }

  /**
   * Generate a list of potion type representations from bukkit's potion type enum to be used with a choice GUI
   * @param name Item name value
   * @param lore Item lore value
   */
  public List<Tuple<Object, ItemStack>> generatePotionTypeReprs(ConfigValue name, ConfigValue lore) {
    // Create a list of all available types
    List<Tuple<Object, ItemStack>> typeReprs = new ArrayList<>();
    for (PotionType type : PotionType.values()) {
      typeReprs.add(new Tuple<>(
        type,
        new ItemStackBuilder(Material.POTION)
          .withName(name.withVariable("type", formatConstant(type.name())))
          .withLore(lore)
          .withBaseEffect(() -> new PotionData(type, false, false), true)
          .build()
      ));
    }

    return typeReprs;
  }


  /**
   * Generate a list of color representations from bukkit's color class to be used with a choice GUI
   * @param material Material resolver function
   * @param name Item name value
   * @param lore Item lore value
   */
  public List<Tuple<Object, ItemStack>> generateColorReprs(Function<Color, Material> material, ConfigValue name, ConfigValue lore) {
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
        new ItemStackBuilder(material.apply(color.b()))
          .withColor(color::b, true)
          .withName(
            name
              .withVariable("color", formatConstant(color.a()))
          )
          .withLore(lore)
          .build()
      ));
    }

    return slotReprs;
  }
}
