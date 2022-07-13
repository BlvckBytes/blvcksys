package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IObjectiveHandler;
import me.blvckbytes.blvcksys.handlers.IPreferencesHandler;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/23/2022

  View and alter your preferences.
*/
@AutoConstruct
public class PreferencesGui extends AGui<Object> {

  private final IPreferencesHandler prefs;
  private final IObjectiveHandler obj;
  private final SingleChoiceGui singleChoiceGui;
  private final IStdGuiItemProvider stdGuiItemProvider;
  private final ILogger logger;

  public PreferencesGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPreferencesHandler prefs,
    @AutoInject IObjectiveHandler obj,
    @AutoInject SingleChoiceGui singleChoiceGui,
    @AutoInject IStdGuiItemProvider stdGuiItemProvider,
    @AutoInject ILogger logger
  ) {
    super(4, "", i -> (
      cfg.get(ConfigKey.GUI_PREFERENCES_TITLE)
        .withVariable("viewer", i.getViewer().getName())
    ), plugin, cfg);

    this.prefs = prefs;
    this.obj = obj;
    this.logger = logger;
    this.singleChoiceGui = singleChoiceGui;
    this.stdGuiItemProvider = stdGuiItemProvider;
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();

    inst.addFill(stdGuiItemProvider);

    // Msg disable
    inst.addStateToggle("19", "10", () -> !prefs.isMsgDisabled(p), s -> prefs.setMsgDisabled(p, s));
    inst.fixedItem("10", () -> (
      new ItemStackBuilder(Material.NAME_TAG)
        .withName(cfg.get(ConfigKey.GUI_PREFERENCES_MSG_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_PREFERENCES_MSG_LORE)
            .withVariable("state", statePlaceholderED(!prefs.isMsgDisabled(p)))
        )
        .build()
    ), null, null);

    // Chat disable
    inst.addStateToggle("20", "11", () -> !prefs.isChatHidden(p), s -> prefs.setChatHidden(p, s));
    inst.fixedItem("11", () -> (
      new ItemStackBuilder(Material.PAPER)
        .withName(cfg.get(ConfigKey.GUI_PREFERENCES_CHAT_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_PREFERENCES_CHAT_LORE)
            .withVariable("state", statePlaceholderED(!prefs.isChatHidden(p)))
        )
        .build()
    ), null, null);

    // Scoreboard hide
    inst.addStateToggle("21", "12", () -> obj.getSidebarVisibility(p), s -> obj.setSidebarVisibility(p, !s));
    inst.fixedItem("12", () -> (
      new ItemStackBuilder(Material.LADDER)
        .withName(cfg.get(ConfigKey.GUI_PREFERENCES_SCOREBOARD_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_PREFERENCES_SCOREBOARD_LORE)
            .withVariable("state", statePlaceholderED(obj.getSidebarVisibility(p)))
        )
        .build()
    ), null, null);

    // Arrow trails
    inst.fixedItem("13", () -> {
      Tuple<@Nullable Particle, @Nullable Color> currTrail = prefs.getArrowTrail(p);
      Particle particle = currTrail.a();
      Color color = currTrail.b();

      String colorStr = cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_NO_COLOR_AVAIL).asScalar();
      if (particle == null)
        colorStr = "/";
      else if (particle.getDataType() == Particle.DustOptions.class)
        colorStr = color == null ? "/" : formatConstant(resolveColorName(color));

      return new ItemStackBuilder(Material.ARROW)
        .withName(cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_LORE)
            .withVariable("particle", particle == null ? "/" : formatConstant(particle.name()))
            .withVariable("color", colorStr)
        )
        .build();
    }, e -> {

      new UserInputChain(inst, values -> {
        Particle particle = (Particle) values.get("particle");

        // Hasn't yet unlocked this particle effect
        if (!hasUnlockedParticle(p, particle)) {
          p.sendMessage(
            cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_PARTICLE_LOCKED)
              .withPrefix()
              .withVariable("particle", formatConstant(particle.name()))
              .asScalar()
          );
          return;
        }

        // Doesn't require any color
        if (particle.getDataType() != Particle.DustOptions.class) {
          prefs.setArrowTrail(p, particle, null);
          return;
        }

        @SuppressWarnings("unchecked")
        Tuple<String, Color> color = (Tuple<String, Color>) values.get("color");
        prefs.setArrowTrail(p, particle, color.b());
      }, singleChoiceGui, null)
        .withChoice(
          "particle",
          cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_PARTICLE_TITLE),
          stdGuiItemProvider, null,
          values -> generateParticleReprs(p),
          null
        )
        .withChoice(
          "color",
          cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_COLOR_TITLE),
          stdGuiItemProvider, null,
          values -> generateColorReprs(this::colorToMaterial),
          values -> {
            // Skip whenever either the particle doesn't support color or the player hasn't yet unlocked this effect
            Particle particle = (Particle) values.get("particle");
            return particle.getDataType() != Particle.DustOptions.class || !hasUnlockedParticle(p, particle);
          }
        )
        .start();
    }, null);

    // Arrow trails clear
    inst.fixedItem("22", () -> {
      boolean hasTrails = prefs.getArrowTrail(p).a() != null;

      return new ItemStackBuilder(hasTrails ? Material.BARRIER : Material.WHITE_STAINED_GLASS_PANE)
        .withName(hasTrails ? cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_RESET_NAME) : null)
        .withLore(hasTrails ? cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_RESET_LORE) : null)
        .build();
    }, e -> {
      // Remove the arrow trail and redraw both the arrow and the remove slot
      prefs.setArrowTrail(p, null, null);
      inst.redraw("22,13");
    }, null);

    // Home laser enable
    inst.addStateToggle("23", "14", () -> prefs.showHomeLasers(p), s -> prefs.setShowHomeLasers(p, !s));
    inst.fixedItem("14", () -> (
      new ItemStackBuilder(Material.BEACON)
        .withName(cfg.get(ConfigKey.GUI_PREFERENCES_HOME_LASERS_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_PREFERENCES_HOME_LASERS_LORE)
            .withVariable("state", statePlaceholderED(prefs.showHomeLasers(p)))
        )
        .build()
    ), null, null);

    return true;
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
   */
  private List<Tuple<Object, ItemStack>> generateColorReprs(Function<Color, Material> material) {
    // Create a list of all available slots
    List<Tuple<Object, ItemStack>> slotReprs = new ArrayList<>();

    List<Triple<Color, String, Material>> colors = getColorConstants().stream()
      .map(t -> new Triple<>(t.a(), t.b(), material.apply(t.a()))).toList();

    for (Triple<Color, String, Material> color : colors) {
      slotReprs.add(new Tuple<>(
        new Tuple<>(color.a(), color.b()),
        // TODO: Implement properly
        new ItemStackBuilder(material.apply(color.a())).build()
//        ies.getItems().getChoices().getColor()
//          .asItem(
//            ConfigValue.makeEmpty()
//              .withVariable("color_hr", formatConstant(color.b()))
//              .withVariable("color", color.b())
//              .withVariable("icon", color.c())
//              .exportVariables()
//          )
//          .build()
      ));
    }

    return slotReprs;
  }

  /**
   * Create all choice particle representitive items and marks them
   * as either accessible or inaccessible, based on the player's permissions
   * @param p Target player
   */
  private List<Tuple<Object, ItemStack>> generateParticleReprs(Player p) {
    List<Tuple<Object, ItemStack>> reprs = new ArrayList<>();

    for (ArrowTrailParticle atp : ArrowTrailParticle.values()) {
      Particle particle = atp.getParticle();

      // Unsupported particle
      if (particle.getDataType() != Void.class && particle.getDataType() != Particle.DustOptions.class)
        continue;

      reprs.add(new Tuple<>(
        particle,
        new ItemStackBuilder(atp.getIcon())
          .withName(
            cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_PARTICLE_NAME)
              .withVariable("particle", formatConstant(particle.name()))
          )
          .withLore(
            cfg.get(hasUnlockedParticle(p, particle) ?
              ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_PARTICLE_LORE_UNLOCKED :
              ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_PARTICLE_LORE_LOCKED
            )
          )
          .build()
      ));
    }

    return reprs;
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
   * Checks whether the player has unlocked the given particle already
   * @param p Target player
   * @param particle Particle to check
   */
  private boolean hasUnlockedParticle(Player p, Particle particle) {
    return PlayerPermission.ARROWTRAILS.has(p, particle.name().toLowerCase());
  }

  /**
   * Resolve a predefined color's name or respond with it's RGB representation
   * @param c Color to resolve
   */
  private String resolveColorName(Color c) {
    return Arrays.stream(Color.class.getDeclaredFields())
      .filter(field -> field.getType().equals(Color.class) && Modifier.isStatic(field.getModifiers()))
      .map(field -> {
        try {
          return new Tuple<>(field.getName(), field.get(null));
        } catch (Exception e) {
          return null;
        }
      })
      .filter(t -> t != null && t.b().equals(c))
      .map(Tuple::a)
      .findFirst()
      .orElse(c.getRed() + ", " + c.getGreen() + ", " + c.getBlue());
  }
}
