package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IObjectiveHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.IPreferencesHandler;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
  private final ItemEditorGui itemEditorGui;

  public PreferencesGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IPreferencesHandler prefs,
    @AutoInject IObjectiveHandler obj,
    @AutoInject SingleChoiceGui singleChoiceGui,
    @AutoInject ItemEditorGui itemEditorGui
  ) {
    super(4, "", i -> (
      cfg.get(ConfigKey.GUI_PREFERENCES_TITLE)
        .withVariable("viewer", i.getViewer().getName())
    ), plugin, cfg, textures);

    this.prefs = prefs;
    this.obj = obj;
    this.itemEditorGui = itemEditorGui;
    this.singleChoiceGui = singleChoiceGui;
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);

    inst.fixedItem(10, () -> (
      new ItemStackBuilder(Material.NAME_TAG)
        .withName(cfg.get(ConfigKey.GUI_PREFERENCES_MSG_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_PREFERENCES_MSG_LORE)
            .withVariable("state", inst.statePlaceholder(!prefs.isMsgDisabled(p)))
        )
        .build()
    ), null);

    inst.fixedItem(12, () -> (
      new ItemStackBuilder(Material.PAPER)
        .withName(cfg.get(ConfigKey.GUI_PREFERENCES_CHAT_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_PREFERENCES_CHAT_LORE)
            .withVariable("state", !prefs.isChatHidden(p))
        )
        .build()
    ), null);

    inst.fixedItem(14, () -> (
      new ItemStackBuilder(Material.LADDER)
        .withName(cfg.get(ConfigKey.GUI_PREFERENCES_SCOREBOARD_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_PREFERENCES_SCOREBOARD_LORE)
            .withVariable("state", obj.getSidebarVisibility(p))
        )
        .build()
    ), null);

    inst.fixedItem(16, () -> {
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
      // Invoke a new single choice gui for available particles
      inst.switchTo(AnimationType.SLIDE_LEFT, singleChoiceGui, new SingleChoiceParam(
        cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_PARTICLE_TITLE).asScalar(), generateParticleReprs(p),
        (part, partInv) -> {
          Particle particle = (Particle) part;

          // Hasn't yet unlocked this particle effect
          if (!hasUnlockedParticle(p, particle)) {
            p.sendMessage(
              cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_PARTICLE_LOCKED)
                .withPrefix()
                .withVariable("particle", formatConstant(particle.name()))
                .asScalar()
            );

            this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, partInv);
            return false;
          }

          // This particle needs dust options (a color)
          if (particle.getDataType() == Particle.DustOptions.class) {
            List<Tuple<Object, ItemStack>> colorReprs = itemEditorGui.generateColorReprs(
              this::colorToMaterial,
              cfg.get(ConfigKey.GUI_PREFERENCES_CHOICE_COLOR_NAME),
              cfg.get(ConfigKey.GUI_PREFERENCES_CHOICE_COLOR_LORE)
            );

            // Invoke a new single choice gui for available colors
            singleChoiceGui.show(p, new SingleChoiceParam(
              cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_COLOR_TITLE).asScalar(), colorReprs,
              (col, colInv) -> {
                @SuppressWarnings("unchecked")
                Tuple<String, Color> color = (Tuple<String, Color>)  col;

                prefs.setArrowTrail(p, particle, color.b());
                this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, colInv);
                return false;
              },
              () -> {}, backInv -> this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, backInv)
            ), AnimationType.SLIDE_LEFT, partInv);

            return false;
          }

          // No color required
          prefs.setArrowTrail(p, particle, null);
          this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, partInv);
          return false;
        },
        () -> {}, inv -> this.show(p, inst.getArg(), AnimationType.SLIDE_RIGHT, inv)
      ));
    });

    inst.addStateToggle(19, 10, () -> !prefs.isMsgDisabled(p), s -> prefs.setMsgDisabled(p, s));
    inst.addStateToggle(21, 12, () -> !prefs.isChatHidden(p), s -> prefs.setChatHidden(p, s));
    inst.addStateToggle(23, 14, () -> obj.getSidebarVisibility(p), s -> obj.setSidebarVisibility(p, !s));

    inst.fixedItem(25, () -> {
      boolean hasTrails = prefs.getArrowTrail(p).a() != null;

      return new ItemStackBuilder(hasTrails ? Material.BARRIER : Material.WHITE_STAINED_GLASS_PANE)
        .withName(hasTrails ? cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_RESET_NAME) : null)
        .withLore(hasTrails ? cfg.get(ConfigKey.GUI_PREFERENCES_ARROW_TRAILS_RESET_LORE) : null)
        .build();
    }, e -> {
      // Remove the arrow trail and redraw both the arrow and the remove slot
      prefs.setArrowTrail(p, null, null);
      inst.redraw("25,16");
    });

    return true;
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
