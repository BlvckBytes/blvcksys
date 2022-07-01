package me.blvckbytes.blvcksys.handlers.gui;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.sections.itemeditor.IESection;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.packets.communicators.bookeditor.IBookEditorCommunicator;
import me.blvckbytes.blvcksys.persistence.models.PlayerTextureModel;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
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
  private final IBookEditorCommunicator bookEditor;
  private final ILogger logger;
  private final ChatUtil chatUtil;
  private final MCReflect refl;
  private final YesNoGui yesNoGui;
  private final MultipleChoiceGui multipleChoiceGui;

  private final IESection ies;

  public ItemEditorGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject SingleChoiceGui singleChoiceGui,
    @AutoInject ILogger logger,
    @AutoInject ChatUtil chatUtil,
    @AutoInject MCReflect refl,
    @AutoInject IBookEditorCommunicator bookEditor,
    @AutoInject YesNoGui yesNoGui,
    @AutoInject MultipleChoiceGui multipleChoiceGui
  ) {
    super(6, "", null, plugin, cfg, textures);

    this.singleChoiceGui = singleChoiceGui;
    this.logger = logger;
    this.chatUtil = chatUtil;
    this.refl = refl;
    this.bookEditor = bookEditor;
    this.yesNoGui = yesNoGui;
    this.multipleChoiceGui = multipleChoiceGui;

    this.ies = cfg.reader("itemeditor")
      .flatMap(r -> r.parseValue(null, IESection.class, true))
      .orElseThrow();

    setTitle(i -> (
      ies.getTitles().getHome()
        .withVariable("item_type", formatConstant(i.getArg().a().getType().name()))
    ));
  }

  @Override
  protected boolean closed(GuiInstance<Triple<ItemStack, @Nullable Consumer<ItemStack>, @Nullable Consumer<GuiInstance<?>>>> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Triple<ItemStack, @Nullable Consumer<ItemStack>, @Nullable Consumer<GuiInstance<?>>>> inst) {
    inst.addFill(ies.getItems().getGeneric().getBackground().build());

    ItemStack item = inst.getArg().a();
    ItemMeta meta = item.getItemMeta();
    Player p = inst.getViewer();

    if (meta == null) {
      p.sendMessage(
        ies.getMessages().getMetaUnavailable()
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

    inst.fixedItem("12,14", () -> (
      ies.getItems().getHome().getDisplayMarker()
        .build()
    ), null);

    // Always keep the edited item in sync with the player's inventory
    inst.fixedItem(13, () -> {
      p.getInventory().setItemInMainHand(item);
      return item;
    }, null);

    // Fire the item update callback whenever the preview slot changes
    inst.onRedrawing(13, () -> {
      Consumer<ItemStack> cb = inst.getArg().b();
      if (cb != null)
        cb.accept(item);
    });

    ///////////////////////////////// Increase Amount //////////////////////////////////


    inst.fixedItem(10, () -> (
      ies.getItems().getHome().getIncrease()
        .build()
    ), e -> {
      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      int amount = item.getAmount();

      if (key == 1)
        amount += 1;

      if (key == 2)
        amount += 64;

      if (key == 3)
        amount += 8;

      if (key == 4)
        amount = 64;

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
      ies.getItems().getHome().getDecrease()
        .build()
    ), e -> {
      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      int amount = item.getAmount();

      if (key == 1)
        amount -= 1;

      if (key == 2)
        amount -= 64;

      if (key == 3)
        amount -= 8;

      if (key == 4)
        amount = 1;

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

    //////////////////////////////// Custom Model Data //////////////////////////////////

    inst.fixedItem(27, () -> (
      ies.getItems().getHome().getCustomModelData()
        .build(
          ConfigValue.makeEmpty()
            .withVariable("custom_model_data", meta.hasCustomModelData() ? meta.getCustomModelData() : "/")
            .exportVariables()
        )
    ), e -> {
      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      // Set the custom model data
      if (key == 1) {
        new UserInputChain(inst, values -> {
          int data = (int) values.get("data");

          meta.setCustomModelData(data);
          item.setItemMeta(meta);
          inst.redraw("13,27");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CUSTOM_MODEL_DATA_SET)
              .withPrefix()
              .withVariable("custom_model_data", data)
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withPrompt(
            "data",
            values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_CUSTOM_MODEL_DATA_PROMPT).withPrefix(),
            Integer::parseInt,
            input -> cfg.get(ConfigKey.ERR_INTPARSE).withVariable("number", input).withPrefix(),
            null
          )
          .start();
        return;
      }

      // Remove the custom model data
      if (key == 2) {
        if (!meta.hasCustomModelData()) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CUSTOM_MODEL_DATA_NOT_SET)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        meta.setCustomModelData(null);
        item.setItemMeta(meta);
        inst.redraw("13,27");

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_CUSTOM_MODEL_DATA_RESET)
            .withPrefix()
            .asScalar()
        );

        return;
      }
    });

    ///////////////////////////////////// Material /////////////////////////////////////

    inst.fixedItem(28, () -> (
      ies.getItems().getHome().getMaterial()
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
      ies.getItems().getHome().getFlags()
        .build(
          ConfigValue.makeEmpty()
            .withVariable("count", meta.getItemFlags().size())
            .exportVariables()
        )
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
      ies.getItems().getHome().getEnchantments()
        .build(
          ConfigValue.makeEmpty()
            .withVariable("count", meta.getEnchants().size())
            .exportVariables()
        )
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
          input -> cfg.get(ConfigKey.ERR_INTPARSE).withVariable("number", input).withPrefix(),
          values -> meta.hasEnchant((Enchantment) values.get("enchantment"))
        )
        .start();
    });

    //////////////////////////////////// Displayname ////////////////////////////////////

    inst.fixedItem(31, () -> (
      ies.getItems().getHome().getDisplayname()
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
      ies.getItems().getHome().getLore()
        .build()
    ), e -> {
      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      // Add a new line
      if (key == 1 || key == 2) {
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
            // Key 2 means push back, no index required
            // Also, if there are no lines yet, just push back too
            values -> key == 2 || lines.size() == 0
          )
          .start();
        return;
      }

      // Remove specific line by choice
      if (key == 3) {
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

      // Reset the lore
      if (key == 4) {

        // Has no lore yet
        if (meta.getLore() == null || meta.getLore().size() == 0) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_LORE_NO_LORE)
              .withPrefix()
              .asScalar()
          );
          return;
        }

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
    });

    //////////////////////////////////// Durability ////////////////////////////////////

    inst.fixedItem(33, () -> {
      boolean isDamageable = (meta instanceof Damageable && item.getType().getMaxDurability() > 0);
      int currDur = item.getType().getMaxDurability() - ((meta instanceof Damageable d) ? d.getDamage() : 0);
      int maxDur = item.getType().getMaxDurability();

      return ies.getItems().getHome().getDurability()
        .patch(ies.getItems().getHome().getNotApplicable(), !isDamageable)
        .build(
          ConfigValue.makeEmpty()
            .withVariable(
              "durability",
              meta.isUnbreakable() ? "Unbreakable" : (
                isDamageable ? (currDur + "/" + maxDur) : "/"
              )
            )
            .exportVariables()
        );
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

      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      // Increase durability
      if (key == 1) {
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

      // Set unbreakable
      if (key == 2) {
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

      // Decrease durability
      if (key == 3) {
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
        return;
      }

      // Remove unbreakability
      if (key == 4) {
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
    });

    ////////////////////////////////////// Attributes //////////////////////////////////////

    inst.fixedItem(34, () -> (
      ies.getItems().getHome().getAttributes()
        .build()
    ), e -> {

      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      // Add an attribute
      if (key == 1) {
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
        return;
      }

      if (key == 2 || key == 3) {
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
        if (key == 3) {
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
    });

    ////////////////////////////////////// Fireworks //////////////////////////////////////

    inst.fixedItem(35, () -> {
      FireworkMeta fMeta = meta instanceof FireworkMeta fm ? fm : null;
      return ies.getItems().getHome().getFireworks()
        .patch(ies.getItems().getHome().getNotApplicable(), fMeta == null)
        .build(
          ConfigValue.makeEmpty()
            .withVariable("power", fMeta == null ? "/" : fMeta.getPower())
            .exportVariables()
        );
    }, e -> {
      if (!(meta instanceof FireworkMeta fm))
        return;

      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      // Set the firework's power
      if (key == 1) {
        new UserInputChain(inst, values -> {
          int power = Math.max(0, (int) values.get("power"));

          fm.setPower(power);
          item.setItemMeta(meta);
          inst.redraw("13,35");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_FIREWORK_POWER_SET)
              .withPrefix()
              .withVariable("power", power)
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withPrompt(
            "power",
            values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_FIREWORK_POWER_PROMPT).withPrefix(),
            Integer::parseInt,
            input -> cfg.get(ConfigKey.ERR_INTPARSE).withVariable("number", input).withPrefix(),
            null
          )
          .start();
        return;
      }

      // Add a new effect
      if (key == 2) {
        new UserInputChain(inst, values ->  {
          FireworkEffect.Type type = (FireworkEffect.Type) values.get("type");
          @SuppressWarnings("unchecked")
          List<Tuple<Color, String>> colors = (List<Tuple<Color, String>>) values.get("color");
          @SuppressWarnings("unchecked")
          List<Tuple<@Nullable Color, String>> fades = (List<Tuple<Color, String>>) values.get("fade");
          boolean flicker = (boolean) values.get("flicker");
          boolean trail = (boolean) values.get("trail");

          FireworkEffect.Builder b = FireworkEffect.builder()
            .with(type)
            .withColor(colors.stream().map(Tuple::a).toArray(Color[]::new))
            .withFade(fades.stream().map(Tuple::a).filter(Objects::nonNull).toArray(Color[]::new))
            .flicker(flicker)
            .trail(trail);

          fm.addEffect(b.build());
          item.setItemMeta(fm);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_FIREWORK_EFFECTS_ADDED)
              .withPrefix()
              .withVariable("type", formatConstant(type.name()))
              .withVariable(
                "colors",
                String.join(
                  cfg.get(ConfigKey.GUI_ITEMEDITOR_FIREWORK_EFFECTS_SEPARATOR).asScalar(),
                  colors.stream().map(t -> formatConstant(t.b())).toList()
                )
              )
              .withVariable(
                "fades",
                String.join(
                  cfg.get(ConfigKey.GUI_ITEMEDITOR_FIREWORK_EFFECTS_SEPARATOR).asScalar(),
                  fades.stream().map(t -> formatConstant(t.b())).toList()
                )
              )
              .withVariable("flicker", formatConstant(String.valueOf(flicker)))
              .withVariable("trail", formatConstant(String.valueOf(trail)))
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "type",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_FIREWORK_TYPE_TITLE),
            this::buildFireworkTypeRepresentitives,
            null
          )
          .withChoice(
            multipleChoiceGui,
            "color",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_FIREWORK_COLOR_TITLE),
            v -> generateColorReprs(
              this::colorToMaterial,
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_COLOR_NAME),
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_COLOR_LORE)
            ),
            null
          )
          .withChoice(
            multipleChoiceGui,
            "fade",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_FIREWORK_FADE_TITLE),
            v -> generateColorReprs(
              this::colorToMaterial,
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_COLOR_NAME),
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_COLOR_LORE),
              true
            ),
            null
          )
          .withYesNo(
            yesNoGui, "flicker",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_YESNO_FLICKER_TITLE),
            cfg.get(ConfigKey.GUI_ITEMEDITOR_YESNO_FLICKER_LORE_YES),
            cfg.get(ConfigKey.GUI_ITEMEDITOR_YESNO_FLICKER_LORE_NO),
            null
          )
          .withYesNo(
            yesNoGui, "trail",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_YESNO_TRAIL_TITLE),
            cfg.get(ConfigKey.GUI_ITEMEDITOR_YESNO_TRAIL_LORE_YES),
            cfg.get(ConfigKey.GUI_ITEMEDITOR_YESNO_TRAIL_LORE_NO),
            null
          )
          .start();
        return;
      }

      // Remove an existing effect
      if (key == 3) {
        if (fm.getEffects().size() == 0) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_FIREWORK_EFFECTS_NONE)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        new UserInputChain(inst, values ->  {
          int index = (int) values.get("index");

          fm.removeEffect(index);
          item.setItemMeta(fm);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_FIREWORK_EFFECTS_REMOVED)
              .withPrefix()
              .withVariable("index", index + 1)
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "index",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_EFFECT_TITLE),
            () -> buildFireworkEffectRepresentitives(fm.getEffects()),
            null
          )
          .start();
        return;
      }

      // Remove all effects
      if (key == 4) {
        if (fm.getEffects().size() == 0) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_FIREWORK_EFFECTS_NONE)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        fm.clearEffects();
        item.setItemMeta(fm);
        inst.redraw("13,35");

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_FIREWORK_EFFECTS_RESET)
            .withPrefix()
            .asScalar()
        );
        return;
      }
    });

    ////////////////////////////////////// Compasses //////////////////////////////////////

    inst.fixedItem(37, () -> {
      CompassMeta cm = meta instanceof CompassMeta x ? x : null;
      return ies.getItems().getHome().getCompass()
        .patch(ies.getItems().getHome().getNotApplicable(), cm == null)
        .build(
          ConfigValue.makeEmpty()
            .withVariable(
              "location",
              (cm != null && cm.getLodestone() != null) ?
                stringifyLocation(cm.getLodestone()) :
                "/"
            )
            .exportVariables()
        );
    }, e -> {
      if (!(meta instanceof CompassMeta cm))
        return;

      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      // Set the target location
      if (key == 1) {
        cm.setLodestone(p.getLocation());
        item.setItemMeta(meta);
        inst.redraw("13,37");

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_COMPASS_LOCATION_SET)
            .withPrefix()
            .withVariable("location", stringifyLocation(p.getLocation()))
            .asScalar()
        );
        return;
      }

      // Remove the target location
      if (key == 2) {
        if (!cm.hasLodestone()) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_COMPASS_LOCATION_NONE)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        cm.setLodestone(null);
        item.setItemMeta(meta);
        inst.redraw("13,37");

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_COMPASS_LOCATION_RESET)
            .withPrefix()
            .asScalar()
        );
        return;
      }
    });

    ///////////////////////////////////// Skull Owner /////////////////////////////////////

    inst.fixedItem(38, () -> {
      SkullMeta sm = meta instanceof SkullMeta x ? x : null;
      return ies.getItems().getHome().getHeadOwner()
        .patch(ies.getItems().getHome().getNotApplicable(), sm == null)
        .build(
          ConfigValue.makeEmpty()
            .withVariable("owner", (sm == null || sm.getOwnerProfile() == null) ? "/" : sm.getOwnerProfile().getName())
            .exportVariables()
        );
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

    inst.fixedItem(39, () -> {
      LeatherArmorMeta lam = meta instanceof LeatherArmorMeta x ? x : null;
      return ies.getItems().getHome().getLeatherColor()
        .patch(ies.getItems().getHome().getNotApplicable(), lam == null)
        .build(
          ConfigValue.makeEmpty()
            .withVariable("color", lam == null ? "/" : stringifyColor(lam.getColor()))
            .exportVariables()
        );
    }, e -> {
      // Not an item which will have leather armor meta
      if (!(meta instanceof LeatherArmorMeta))
        return;

      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      if (key == 1 || key == 2) {
        promptForColor(inst, key == 2, item, meta);
        return;
      }

      if (key == 3) {
        resetColor(inst, item, meta);
        return;
      }
    });

    ///////////////////////////////////// Potion Effects /////////////////////////////////////

    inst.fixedItem(40, () -> (
      ies.getItems().getHome().getPotionEffects()
        .patch(ies.getItems().getHome().getNotApplicable(), !(meta instanceof PotionMeta))
        .build()
    ), e -> {
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
            input -> cfg.get(ConfigKey.ERR_INTPARSE).withVariable("number", input).withPrefix(),
            null
          )
          .withPrompt(
            "amplifier",
            values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_AMPLIFIER_PROMPT)
              .withPrefix(),
            Integer::parseInt,
            input -> cfg.get(ConfigKey.ERR_INTPARSE).withVariable("number", input).withPrefix(),
            null
          )
          .start();

        return;
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

        return;
      }

      // Clear all secondary effects
      if (key == 6) {
        if (!potionMeta.clearCustomEffects()) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_NO_SECONDARY)
              .withPrefix()
              .asScalar()
          );
          return;
        }

        item.setItemMeta(meta);
        inst.redraw("13");

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_POTIONEFFECTS_SECONDARIES_CLEARED)
            .withPrefix()
            .asScalar()
        );
      }

      // Change color
      if (key == 7 || key == 8) {
        promptForColor(inst, key == 8, item, meta);
        return;
      }

      // Reset color
      if (key == 9) {
        resetColor(inst, item, meta);
        return;
      }
    });

    //////////////////////////////////////// Maps ////////////////////////////////////////

    inst.fixedItem(41, () -> (
      ies.getItems().getHome().getMaps()
        .patch(ies.getItems().getHome().getNotApplicable(), !(meta instanceof MapMeta))
        .build()
    ), e -> {
      // Not an item which will have map meta
      if (!(meta instanceof MapMeta mapMeta))
        return;

      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      // Change color
      if (key == 1 || key == 2) {
        promptForColor(inst, key == 2, item, meta);
        return;
      }

      // Reset color
      if (key == 3) {
        resetColor(inst, item, meta);
        return;
      }
    });

    //////////////////////////////////////// Books ////////////////////////////////////////

    inst.fixedItem(42, () -> (
      ies.getItems().getHome().getBooks()
        .patch(ies.getItems().getHome().getNotApplicable(), !(meta instanceof BookMeta))
        .build()
    ), e -> {
      // Not an item which will have book meta
      if (!(meta instanceof BookMeta bookMeta))
        return;

      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      // Set title
      if (key == 1 || key == 2) {
        // Reset
        if (key == 2) {
          if (bookMeta.getTitle() == null) {
            p.sendMessage(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_TITLE_NOT_SET)
                .withPrefix()
                .asScalar()
            );
            return;
          }

          bookMeta.setTitle(null);
          item.setItemMeta(bookMeta);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_TITLE_RESET)
              .withPrefix()
              .asScalar()
          );

          return;
        }

        promptPlainText(inst, cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_TITLE_PROMPT), title -> {
          bookMeta.setTitle(title);
          item.setItemMeta(bookMeta);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_TITLE_SET)
              .withPrefix()
              .withVariable("title", title)
              .asScalar()
          );
        });
        return;
      }

      // Set author
      if (key == 3 || key == 4) {
        // Reset
        if (key == 4) {
          if (bookMeta.getAuthor() == null) {
            p.sendMessage(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_AUTHOR_NOT_SET)
                .withPrefix()
                .asScalar()
            );
            return;
          }

          bookMeta.setAuthor(null);
          item.setItemMeta(bookMeta);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_AUTHOR_RESET)
              .withPrefix()
              .asScalar()
          );

          return;
        }

        promptPlainText(inst, cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_AUTHOR_PROMPT), author -> {
          bookMeta.setAuthor(author);
          item.setItemMeta(bookMeta);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_AUTHOR_SET)
              .withPrefix()
              .withVariable("author", author)
              .asScalar()
          );
        });
        return;
      }

      // Set generation
      if (key == 5 || key == 6) {
        // Reset
        if (key == 6) {
          if (bookMeta.getGeneration() == null) {
            p.sendMessage(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_GENERATION_NOT_SET)
                .withPrefix()
                .asScalar()
            );
            return;
          }

          bookMeta.setGeneration(null);
          item.setItemMeta(bookMeta);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_GENERATION_RESET)
              .withPrefix()
              .asScalar()
          );

          return;
        }

        new UserInputChain(inst, values -> {
          BookMeta.Generation gen = (BookMeta.Generation) values.get("generation");

          bookMeta.setGeneration(gen);
          item.setItemMeta(bookMeta);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_GENERATION_SET)
              .withPrefix()
              .withVariable("generation", formatConstant(gen.name()))
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "generation",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_GENERATION_TITLE),
            this::buildGenerationRepresentitives,
            null
          )
          .start();
        return;
      }

      // Edit pages
      if (key == 7) {
        new UserInputChain(inst, values -> {
          @SuppressWarnings("unchecked")
          List<String> result = (List<String>) values.get("result");

          bookMeta.setPages(result);
          item.setItemMeta(meta);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_PAGES_EDITED)
              .withPrefix()
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withBookEditor(
            bookEditor, "result",
            values -> cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_PAGES_EDIT_PROMPT).withPrefix(),
            values -> bookMeta.getPages()
          )
          .start();
        return;
      }

      // Remove existing page
      if (key == 8) {
        List<String> pages = new ArrayList<>(bookMeta.getPages());

        // Cannot remove the only page
        if (pages.size() == 1) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_PAGES_SINGLE)
              .withPrefixes()
              .asScalar()
          );
          return;
        }

        new UserInputChain(inst, values -> {
          int index = (int) values.get("index");

          pages.remove(index);
          bookMeta.setPages(pages);
          item.setItemMeta(meta);

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BOOK_PAGE_REMOVED)
              .withPrefix()
              .withVariable("page_number", index + 1)
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "index",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_PAGE_TITLE),
            () -> buildPageRepresentitives(bookMeta.getPages()),
            null
          )
          .start();
        return;
      }
    });

    //////////////////////////////////////// Banners ////////////////////////////////////////

    inst.fixedItem(43, () -> (
      ies.getItems().getHome().getBanners()
        .patch(ies.getItems().getHome().getNotApplicable(), !(meta instanceof BannerMeta))
        .build()
    ), e -> {
      if (!(meta instanceof BannerMeta bm))
        return;

      Integer key = e.getHotbarKey().orElse(null);
      if (key == null)
        return;

      // Add a new pattern
      if (key == 1) {
        new UserInputChain(inst, values -> {
          PatternType type = (PatternType) values.get("type");
          DyeColor color = (DyeColor) values.get("color");

          bm.addPattern(new Pattern(color, type));
          item.setItemMeta(meta);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BANNER_PATTERNS_ADDED)
              .withPrefix()
              .withVariable("type", formatConstant(type.name()))
              .withVariable("color", formatConstant(color.name()))
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "type",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_PATTERN_TYPE_TITLE),
            () -> buildBannerPatternTypeRepresentitives(item.getType()),
            null
          )
          .withChoice(
            "color",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_DYE_COLOR_TITLE),
            v -> buildBannerDyeColorRepresentitives(item.getType(), (PatternType) v.get("type")),
            null
          )
          .start();
        return;
      }

      // Delete a pattern
      if (key == 2) {
        List<Pattern> patterns = new ArrayList<>(bm.getPatterns());

        if (patterns.size() == 0) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BANNER_PATTERNS_NONE)
              .withPrefixes()
              .asScalar()
          );
          return;
        }

        new UserInputChain(inst, values -> {
          int index = (int) values.get("index");

          patterns.remove(index);
          bm.setPatterns(patterns);
          item.setItemMeta(meta);
          inst.redraw("13");

          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BANNER_PATTERNS_REMOVED)
              .withPrefix()
              .withVariable("index", index + 1)
              .asScalar()
          );
        }, singleChoiceGui, chatUtil)
          .withChoice(
            "index",
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_PATTERNS_TITLE),
            () -> buildBannerPatternRepresentitives(item.getType(), patterns),
            null
          )
          .start();
        return;
      }

      // Delete all pattern
      if (key == 3) {
        if (bm.getPatterns().size() == 0) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_BANNER_PATTERNS_NONE)
              .withPrefixes()
              .asScalar()
          );
          return;
        }

        bm.setPatterns(new ArrayList<>());
        item.setItemMeta(meta);
        inst.redraw("13");

        p.sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_BANNER_PATTERNS_CLEARED)
            .withPrefix()
            .asScalar()
        );
        return;
      }
    });

    return true;
  }

  /**
   * Wraps a given text on multiple lines by counting the chars per line
   * @param text Text to wrap in length
   * @param charsPerLine How many chars to display per line
   * @return Wrapped text
   */
  private String wrapText(String text, int charsPerLine) {
    StringBuilder res = new StringBuilder();
    int remChPerLine = charsPerLine;
    boolean isFirstLine = true;

    for (String word : text.split(" ")) {
      int wlen = word.length();

      if ((isFirstLine && wlen > remChPerLine / 2) || wlen > remChPerLine) {
        isFirstLine = false;
        res.append("\n").append(word).append(" ");
        remChPerLine = charsPerLine;
        continue;
      }

      res.append(word).append(" ");
      remChPerLine -= wlen + 1;
    }

    return res.toString();
  }

  /**
   * Promts the user for a plain text input without any validation
   * @param inst GUI instance
   * @param prompt Prompt message
   * @param value Value input callback
   */
  private void promptPlainText(GuiInstance<?> inst, ConfigValue prompt, Consumer<String> value) {
    new UserInputChain(inst, values -> value.accept((String) values.get("value")), singleChoiceGui, chatUtil)
      .withPrompt(
        "value",
        values -> prompt,
        s -> ChatColor.translateAlternateColorCodes('&', s), null, null
      )
      .start();
  }

  /**
   * Tries to set the color of an item, does nothing if it's uncolorable,
   * notifies the player internally
   * @param inst GUI instance
   * @param item Target item
   * @param meta Target item's meta
   * @param color Color to set
   */
  private void setColor(GuiInstance<?> inst, ItemStack item, ItemMeta meta, Tuple<Color, String> color) {
    try {
      refl.invokeMethodByName(meta, "setColor", new Class[]{ Color.class }, color == null ? null : color.a());
      item.setItemMeta(meta);
      inst.redraw("13");

      if (color == null) {
        inst.getViewer().sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_COLOR_RESET)
            .withPrefix()
            .asScalar()
        );

        return;
      }

      inst.getViewer().sendMessage(
        cfg.get(ConfigKey.GUI_ITEMEDITOR_COLOR_CHANGED)
          .withPrefix()
          .withVariable("color", formatConstant(color.b()))
          .asScalar()
      );
    }

    // Cannot change the color of this item, do nothing
    catch (Exception ignored) {}
  }

  /**
   * Tries to reset the color of an item, does nothing if it's uncolorable,
   * re-sets the color if one is set, notifies the player internally
   * @param inst GUI instance
   * @param item Target item
   * @param meta Target item's meta
   */
  private void resetColor(GuiInstance<?> inst, ItemStack item, ItemMeta meta) {
    try {
      Color color = (Color) refl.invokeMethodByName(meta, "getColor", new Class<?>[]{});

      // Has no color applied yet (leather always has a color - brown)
      if (color == null || (meta instanceof LeatherArmorMeta && color.equals(Color.fromRGB(0xA06540)))) {
        inst.getViewer().sendMessage(
          cfg.get(ConfigKey.GUI_ITEMEDITOR_COLOR_NONE)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      // Reset the color
      setColor(inst, item, meta, null);
    }

    // Cannot change the color of this item, do nothing
    catch (Exception ignored) {}
  }

  /**
   * Prompts the user for a color by either offering a choice within a
   * pre-defined list or by prompting for RGB values using the chat and then
   * applies that received color to the item, notifies the player internally
   * @param inst GUI instance
   * @param custom Whether to prompt for RGB
   * @param item Target item
   * @param meta Target item's meta
   */
  @SuppressWarnings("unchecked")
  private void promptForColor(GuiInstance<?> inst, boolean custom, ItemStack item, ItemMeta meta) {
    UserInputChain chain = new UserInputChain(inst, values -> {
      Tuple<Color, String> color = (Tuple<Color, String>) values.get("color");
      setColor(inst, item, meta, color);
    }, singleChoiceGui, chatUtil);

    // Custom RGB color input by prompt
    if (custom) {
      chain.withPrompt(
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
      );
    }

    // Select from a list of predefined colors
    else {
      chain.withChoice(
        "color",
        cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_COLOR_TITLE),
        () -> generateColorReprs(
          c -> item.getType(),
          cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_COLOR_NAME),
          cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_COLOR_LORE)
        ),
        null
      );
    }

    chain.start();
  }

  /**
   * Maps pre-defined bukkit colors to somewhat matching materials to use as icons.
   * @param c Color to map
   * @return Displayable material
   */
  private Material colorToMaterial(Color c) {
    if (c == Color.WHITE)
      return Material.WHITE_TERRACOTTA;

    if (c == Color.SILVER)
      return Material.LIGHT_GRAY_TERRACOTTA;

    if (c == Color.GRAY)
      return Material.GRAY_TERRACOTTA;

    if (c == Color.BLACK)
      return Material.BLACK_TERRACOTTA;

    if (c == Color.RED)
      return Material.RED_TERRACOTTA;

    if (c == Color.MAROON)
      return Material.PINK_TERRACOTTA;

    if (c == Color.YELLOW)
      return Material.YELLOW_TERRACOTTA;

    if (c == Color.OLIVE)
      return Material.CYAN_TERRACOTTA;

    if (c == Color.LIME)
      return Material.LIME_TERRACOTTA;

    if (c == Color.GREEN)
      return Material.GREEN_TERRACOTTA;

    if (c == Color.AQUA || c == Color.TEAL)
      return Material.LIGHT_BLUE_TERRACOTTA;

    if (c == Color.NAVY || c == Color.BLUE)
      return Material.BLUE_TERRACOTTA;

    if (c == Color.FUCHSIA)
      return Material.BROWN_TERRACOTTA;

    if (c == Color.PURPLE)
      return Material.PURPLE_TERRACOTTA;

    if (c == Color.ORANGE)
      return Material.ORANGE_TERRACOTTA;

    return Material.LIGHT_GRAY_TERRACOTTA;
  }

  /**
   * Builds a list of representitives for all available banner pattern types
   * @param base Base material to lay patterns onto for representitives
   */
  private List<Tuple<Object, ItemStack>> buildBannerPatternTypeRepresentitives(Material base) {
    return Arrays.stream(PatternType.values()).map(type -> (
      new Tuple<>(
        (Object) type,
        new ItemStackBuilder(base, 1)
          .setPattern(
            // Check the banner color to at least avoid horrible contrasts
            type, (
              base == Material.WHITE_BANNER ||
              base == Material.GRAY_BANNER ||
              base == Material.LIGHT_GRAY_BANNER
            ) ? DyeColor.BLACK : DyeColor.WHITE
          )
          .withName(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_PATTERN_TYPE_NAME)
              .withVariable("type", formatConstant(type.name()))
          )
          .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_PATTERN_TYPE_LORE))
          .build()
      )
    )).toList();
  }

  /**
   * Builds a list of representitives for all available dye colors
   * @param base Base material to use for the icon
   * @param type Pattern type to show in different colors
   */
  private List<Tuple<Object, ItemStack>> buildBannerDyeColorRepresentitives(Material base, PatternType type) {
    return Arrays.stream(DyeColor.values()).map(color -> (
      new Tuple<>(
        (Object) color,
        new ItemStackBuilder(base, 1)
          .setPattern(type, color)
          .withName(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_DYE_COLOR_NAME)
              .withVariable("color", formatConstant(color.name()))
          )
          .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_DYE_COLOR_LORE))
          .build()
      )
    )).toList();
  }

  /**
   * Builds a list of representitives for all provided banner patterns
   * @param base Base material to lay patterns onto for representitives
   * @param patterns Patterns to build for
   */
  private List<Tuple<Object, ItemStack>> buildBannerPatternRepresentitives(Material base, List<Pattern> patterns) {
    return patterns.stream().map(pattern -> (
      new Tuple<>(
        (Object) patterns.indexOf(pattern),
        new ItemStackBuilder(base, 1)
          .setPattern(pattern.getPattern(), pattern.getColor())
          .withName(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_PATTERNS_NAME)
              .withVariable("index", patterns.indexOf(pattern) + 1)
          )
          .withLore(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_PATTERNS_LORE)
              .withVariable("type", formatConstant(pattern.getPattern().name()))
              .withVariable("color", formatConstant(pattern.getColor().name()))
          )
          .build()
      )
    )).toList();
  }

  /**
   * Builds a list of representitives for all provided firework effects
   * @param effects Effects to build for
   */
  private List<Tuple<Object, ItemStack>> buildFireworkEffectRepresentitives(List<FireworkEffect> effects) {
    return effects.stream()
      .map(effect -> (
        new Tuple<>(
          (Object) effects.indexOf(effect),
          new ItemStackBuilder(resolveFireworkEffectTypeIcon(effect.getType()), 1)
            .withName(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_EFFECT_NAME)
                .withVariable("index", effects.indexOf(effect) + 1)
            )
            .withLore(
              cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_EFFECT_LORE)
                .withVariable("type", formatConstant(effect.getType().name()))
                .withVariable(
                  "colors",
                  String.join(
                    cfg.get(ConfigKey.GUI_ITEMEDITOR_FIREWORK_EFFECTS_SEPARATOR).asScalar(),
                    effect.getColors().stream().map(this::stringifyColor).toList()
                  )
                )
                .withVariable(
                  "fades",
                  String.join(
                    cfg.get(ConfigKey.GUI_ITEMEDITOR_FIREWORK_EFFECTS_SEPARATOR).asScalar(),
                    effect.getFadeColors().stream().map(this::stringifyColor).toList()
                  )
                )
                .withVariable("flicker", formatConstant(String.valueOf(effect.hasFlicker())))
                .withVariable("trail", formatConstant(String.valueOf(effect.hasTrail())))
            )
            .build()
        )
      )).toList();
  }

  /**
   * Resolves a given firework effect type to it's representitive icon
   * @param type Firework effect type
   * @return Material to display for this effect
   */
  private Material resolveFireworkEffectTypeIcon(FireworkEffect.Type type) {
    return switch (type) {
      case STAR -> Material.NETHER_STAR;
      case CREEPER -> Material.CREEPER_HEAD;
      case BURST -> Material.TNT;
      case BALL -> Material.SUNFLOWER;
      case BALL_LARGE -> Material.SNOWBALL;
      default -> Material.FIREWORK_STAR;
    };
  }

  /**
   * Builds a list of representitives for all available firework types
   */
  private List<Tuple<Object, ItemStack>> buildFireworkTypeRepresentitives() {
    // Create representitive items for each firework type
    List<Tuple<Object, ItemStack>> representitives = new ArrayList<>();

    for (FireworkEffect.Type type : FireworkEffect.Type.values()) {
      representitives.add(new Tuple<>(
        type,
        new ItemStackBuilder(resolveFireworkEffectTypeIcon(type))
          .withName(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_FIREWORK_TYPE_NAME)
              .withVariable("type", formatConstant(type.name()))
          )
          .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_FIREWORK_TYPE_LORE))
          .build()
      ));
    }

    return representitives;
  }

  /**
   * Builds a list of representitives for all available book generations
   */
  private List<Tuple<Object, ItemStack>> buildGenerationRepresentitives() {
    // Create representitive items for each generation
    List<Tuple<Object, ItemStack>> representitives = new ArrayList<>();

    for (BookMeta.Generation generation : BookMeta.Generation.values()) {
      representitives.add(new Tuple<>(
        generation,
        new ItemStackBuilder(Material.BOOK)
          .withName(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_GENERATION_NAME)
              .withVariable("generation", formatConstant(generation.name()))
          )
          .withLore(cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_GENERATION_LORE))
          .build()
      ));
    }

    return representitives;
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
   * Build a list of representitives for all available pages within a book
   * @param pages List of book pages
   */
  private List<Tuple<Object, ItemStack>> buildPageRepresentitives(List<String> pages) {
    // Create representitive items for each page
    List<Tuple<Object, ItemStack>> representitives = new ArrayList<>();
    for (int i = 0; i < pages.size(); i++) {
      String page = pages.get(i);
      representitives.add(new Tuple<>(
        i,
        new ItemStackBuilder(Material.PAPER)
          .withName(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_PAGE_NAME)
              .withVariable("page_number", i + 1)
          )
          .withLore(
            cfg.get(ConfigKey.GUI_ITEMEDITOR_CHOICE_PAGE_LORE)
              .withVariable("page_content", wrapText(page, 35))
          )
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
    return generateColorReprs(material, name, lore, false);
  }

  /**
   * Stringifies a location to be human readable
   * @param loc Location to stringify
   */
  private String stringifyLocation(Location loc) {
    return ConfigValue.immediate("{{x}}, {{y}}, {{z}}, {{world}}")
      .withVariable("x", loc.getX())
      .withVariable("y", loc.getY())
      .withVariable("z", loc.getZ())
      .withVariable("world", loc.getWorld() == null ? "?" : loc.getWorld().getName())
      .asScalar();
  }

  /**
   * Stringify a color to either it's predefined constant name, or it's custom RGB representation
   * @param color Color to stringify
   */
  private String stringifyColor(Color color) {
    // Try to find a color by it's predefined constant
    Tuple<Color, String> name = getColorConstants().stream()
      .filter(t -> t.a().equals(color))
      .findFirst()
      .orElse(null);

    // No constant available
    if (name == null)
      return color.getRed() + " " + color.getGreen() + " " + color.getBlue();

    return formatConstant(name.b());
  }

  /**
   * Get a list of all pre-defined color constants and their names
   */
  private List<Tuple<Color, String>> getColorConstants() {
    List<Tuple<Color, String>> colors = new ArrayList<>();

    // Get all available colors from the class's list of constant fields
    try {
      List<Field> constants = Arrays.stream(Color.class.getDeclaredFields())
        .filter(field -> field.getType().equals(Color.class) && Modifier.isStatic(field.getModifiers()))
        .toList();

      for (Field constant : constants) {
        Color color = (Color) constant.get(null);

        if (color == null)
          continue;

        colors.add(new Tuple<>(color, constant.getName()));
      }
    } catch (Exception ex) {
      logger.logError(ex);
    }

    return colors;
  }

  /**
   * Generate a list of color representations from bukkit's color class to be used with a choice GUI
   * @param material Material resolver function
   * @param name Item name value
   * @param lore Item lore value
   * @param withTransparent Whether to offer a choice of a transparent color, which will return null
   */
  public List<Tuple<Object, ItemStack>> generateColorReprs(Function<Color, Material> material, ConfigValue name, ConfigValue lore, boolean withTransparent) {
    // Create a list of all available slots
    List<Tuple<Object, ItemStack>> slotReprs = new ArrayList<>();
    for (Tuple<Color, String> color : getColorConstants()) {
      slotReprs.add(new Tuple<>(
        color,
        new ItemStackBuilder(material.apply(color.a()))
          .withColor(color::a, true)
          .withName(
            name
              .withVariable("color", formatConstant(color.b()))
          )
          .withLore(lore)
          .build()
      ));
    }

    // Offer a transparent choice (represents null-color)
    if (withTransparent) {
      slotReprs.add(new Tuple<>(
        new Tuple<Color, String>(null, "Transparent"),
        new ItemStackBuilder(Material.GLASS)
          .withName(
            name
              .withVariable("color", formatConstant("Transparent"))
          )
          .withLore(lore)
          .build()
      ));
    }

    return slotReprs;
  }
}
