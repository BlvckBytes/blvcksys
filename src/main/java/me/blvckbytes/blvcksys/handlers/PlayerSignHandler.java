package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.PlayerSignModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldQueryGroup;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  Handles managing a sign's state per player and decides when
  to render signs based on the distance.
*/
@AutoConstruct
public class PlayerSignHandler extends ATemplateHandler implements IPlayerSignHandler, IAutoConstructed, Listener {

  // Specifies the time between sign update triggers in ticks
  private static final long UPDATE_INTERVAL_TICKS = 20;

  // Specify the max. squared distance between the sign and any
  // given recipient that receives updates here
  private static final double RECIPIENT_MAX_DIST_SQ = Math.pow(30, 2);

  // Template cache, mapping the sign location to a tuple of the minimum required
  // update period of all sign lines and a list of line templates
  private final Map<Location, Tuple<Long, List<List<Object>>>> templates;
  private int intervalHandle;

  private final IPersistence pers;
  private final JavaPlugin plugin;
  private final IConfig cfg;

  public PlayerSignHandler(
    @AutoInject IPersistence pers,
    @AutoInject JavaPlugin plugin,
    @AutoInject ILiveVariableSupplier varSupp,
    @AutoInject IConfig cfg
  ) {
    super(varSupp);

    this.pers = pers;
    this.plugin = plugin;
    this.cfg = cfg;

    this.intervalHandle = -1;
    this.templates = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public Optional<PlayerSignModel> createSign(OfflinePlayer creator, Sign sign) {
    if (pers.count(buildQuery(sign.getLocation())) > 0)
      return Optional.empty();

    PlayerSignModel model = new PlayerSignModel(creator, null, sign.getLocation(), "", "", "", "");
    pers.store(model);
    updateTemplate(model);
    return Optional.of(model);
  }

  @Override
  public Optional<PlayerSignModel> findSign(Sign sign) {
    return pers.findFirst(buildQuery(sign.getLocation()));
  }

  @Override
  public boolean editSign(OfflinePlayer editor, Sign sign, String line, int lineIndex) {
    PlayerSignModel model = pers.findFirst(buildQuery(sign.getLocation())).orElse(null);

    if (model == null)
      return false;

    if (lineIndex <= 0 || lineIndex > 4)
      lineIndex = 0;

    switch (lineIndex) {
      case 1 -> model.setLine1(line);
      case 2 -> model.setLine2(line);
      case 3 -> model.setLine3(line);
      case 4 -> model.setLine4(line);
    }

    model.setLastEditor(editor);
    pers.store(model);
    updateTemplate(model);
    return true;
  }

  @Override
  public TriResult moveSign(OfflinePlayer editor, Sign from, Sign to) {
    PlayerSignModel targetSign = pers.findFirst(buildQuery(from.getLocation())).orElse(null);

    if (targetSign == null)
      return TriResult.EMPTY;

    PlayerSignModel toSign = pers.findFirst(buildQuery(to.getLocation())).orElse(null);

    if (toSign != null)
      return TriResult.ERR;

    targetSign.setLoc(to.getLocation());
    targetSign.setLastEditor(editor);
    pers.store(targetSign);

    // Mark this sign for ASAP restore and deletion
    templates.put(from.getLocation(), new Tuple<>(1L, null));

    updateTemplate(targetSign);
    return TriResult.SUCC;
  }

  @Override
  public boolean deleteSign(Sign sign) {
    templates.remove(sign.getLocation());
    return pers.delete(buildQuery(sign.getLocation())) > 0;
  }

  @Override
  public void cleanup() {
    if (this.intervalHandle > 0)
      Bukkit.getScheduler().cancelTask(this.intervalHandle);
    templates.clear();
  }

  @Override
  public void initialize() {
    // Load all signs into the template cache initially
    for (PlayerSignModel sign : pers.list(PlayerSignModel.class))
      updateTemplate(sign);

    this.intervalHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {

      long time = 0;

      @Override
      public void run() {
        for (Iterator<Location> li = templates.keySet().iterator(); li.hasNext();) {
          Location loc = li.next();
          Tuple<Long, List<List<Object>>> data = templates.get(loc);

          // Only tick when there's a sign block and when the min line template period has elapsed
          if ((loc.getBlock().getState() instanceof Sign s) && time % data.a() == 0)
            tickSign(s, data.b());

          // This sign was marked for deletion
          if (data.b() == null)
            li.remove();
        }
        time += UPDATE_INTERVAL_TICKS;
      }
    }, 0L, UPDATE_INTERVAL_TICKS);
  }

  //=========================================================================//
  //                                Listener                                 //
  //=========================================================================//

  @EventHandler
  public void onBreak(BlockBreakEvent e) {
    if (!templates.containsKey(e.getBlock().getLocation()))
      return;

    e.setCancelled(true);
    e.getPlayer().sendMessage(
      cfg.get(ConfigKey.PSIGN_CANNOT_BREAK)
        .withPrefix()
        .asScalar()
    );
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Update the locally stored line templates for a sign
   * @param sign Sign to update the template for
   */
  private void updateTemplate(PlayerSignModel sign) {
    Long dur = Long.MAX_VALUE;
    List<List<Object>> lineTemplates = new ArrayList<>();

    // Build a list of line templates from the string lines and
    // store the lowest period among all of them
    for (String line : List.of(sign.getLine1(), sign.getLine2(), sign.getLine3(), sign.getLine4())) {
      Tuple<Long, List<Object>> lineTemplate = buildLineTemplate(line);
      lineTemplates.add(lineTemplate.b());

      if (lineTemplate.a() < dur)
        dur = lineTemplate.a();
    }

    templates.put(sign.getLoc(), new Tuple<>(dur, lineTemplates));
  }

  /**
   * Updates a player sign for all near players
   * @param sign Sign to update
   * @param template Template of the sign, null to restore the sign
   */
  private void tickSign(Sign sign, @Nullable List<List<Object>> template) {
    for (Player recipient : Bukkit.getOnlinePlayers()) {
      if (!isRecipient(sign.getLocation(), recipient))
        continue;

      String[] lines;
      if (template == null)
        lines = sign.getLines();

      else {
        lines = template.stream()
          .map(tp -> evaluateLineTemplate(recipient, tp))
          .toArray(String[]::new);
      }

      recipient.sendSignChange(sign.getLocation(), lines);
    }
  }

  /**
   * Checks if the given player is a recipient of this sign
   * @param p Player to test for
   * @return True if should receive updates, false otherwise
   */
  private boolean isRecipient(Location loc, Player p) {
    Location pLoc = p.getLocation();

    // Not in the same world
    if (loc.getWorld() != pLoc.getWorld())
      return false;

    // Check if the player is within reach
    return loc.distanceSquared(pLoc) <= RECIPIENT_MAX_DIST_SQ;
  }

  /**
   * Builds a query to select a sign by it's location
   * @param loc Location of the sign
   */
  private QueryBuilder<PlayerSignModel> buildQuery(Location loc) {
    String world = loc.getWorld() == null ? "?" : loc.getWorld().getName();
    return new QueryBuilder<>(
      PlayerSignModel.class,
      new FieldQueryGroup(
        "loc__x", EqualityOperation.EQ, loc.getX()
      )
        .and("loc__y", EqualityOperation.EQ, loc.getY())
        .and("loc__z", EqualityOperation.EQ, loc.getZ())
        .and("loc__world", EqualityOperation.EQ, world)
    );
  }
}
