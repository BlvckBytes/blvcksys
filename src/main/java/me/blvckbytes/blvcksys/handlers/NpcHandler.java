package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.events.NpcInteractEvent;
import me.blvckbytes.blvcksys.events.NpcInteraction;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.packets.ModificationPriority;
import me.blvckbytes.blvcksys.packets.communicators.npc.INpcCommunicator;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.NpcModel;
import me.blvckbytes.blvcksys.persistence.models.PlayerTextureModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldQueryGroup;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInUseEntity;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/20/2022

  Handles spawning npcs for players within a given radius and catches
  interactions to fire custom npc events.
*/
@AutoConstruct
public class NpcHandler implements INpcHandler, IAutoConstructed, IPacketModifier, Listener {

  // Specifies the max. distance when searching for the nearest npc
  // handle horizontally and vertically, where the horizontal distance should be
  // a lot greater, as holograms which are overlapping with npc hitboxes usually are
  // almost on the same x- and z coordinates and only differ mostly in y.
  private static final double NEAR_NPC_MAX_DIST_H = 1;
  private static final double NEAR_NPC_MAX_DIST_V = 4;

  // Specifies the time between npc update triggers in ticks
  private static final long UPDATE_INTERVAL_TICKS = 10;

  // Time in milliseconds to use for event debouncing
  private static final long EVENT_DEBOUNCE_MS = 50;

  private final JavaPlugin plugin;
  private final IPersistence pers;
  private final INpcCommunicator npcComm;
  private final ILogger logger;
  private final MCReflect refl;
  private final IPlayerTextureHandler playerTextures;

  // Mapping npc-names to fake-npcs
  private final Map<String, FakeNpc> npcs;

  // Mapping entity IDs to fake-npcs
  private final Map<Integer, FakeNpc> npcIds;

  // Mapping event causing players to their last event emit timestamp (for debouncing)
  private final Map<UUID, Long> lastEventEmits;

  private int intervalHandle;

  public NpcHandler(
    @AutoInject JavaPlugin plugin,
    @AutoInject IPersistence pers,
    @AutoInject INpcCommunicator npcComm,
    @AutoInject ILogger logger,
    @AutoInject IPacketInterceptor interceptor,
    @AutoInject MCReflect refl,
    @AutoInject PlayerTextureHandler playerTextures
  ) {
    this.plugin = plugin;
    this.pers = pers;
    this.npcComm = npcComm;
    this.refl = refl;
    this.logger = logger;
    this.playerTextures = playerTextures;

    this.intervalHandle = -1;
    this.npcs = new HashMap<>();
    this.npcIds = new HashMap<>();
    this.lastEventEmits = Collections.synchronizedMap(new HashMap<>());

    interceptor.register(this, ModificationPriority.LOW);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public Optional<NpcModel> createNpc(OfflinePlayer creator, String name, Location loc) {
    try {
      NpcModel npc = new NpcModel(creator, name, loc, null);
      pers.store(npc);

      FakeNpc fNpc = fakeNpcFromModel(npc);
      npcs.put(name.toLowerCase(), fNpc);
      npcIds.put(fNpc.getEntityId(), fNpc);

      return Optional.of(npc);
    } catch (DuplicatePropertyException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean deleteNpc(String name) {
    boolean res = pers.delete(buildQuery(name)) > 0;

    if (res)
      npcs.remove(name.toLowerCase()).destroy();

    return res;
  }

  @Override
  public boolean moveNpc(String name, Location loc) {
    NpcModel npc = pers.findFirst(buildQuery(name)).orElse(null);

    if (npc == null)
      return false;

    npc.setLoc(loc);
    pers.store(npc);

    findFakeNpcByModel(npc).setLoc(loc);
    return true;
  }

  @Override
  public TriResult changeSkin(String name, String skin) {
    NpcModel npc = pers.findFirst(buildQuery(name)).orElse(null);

    if (npc == null)
      return TriResult.EMPTY;

    PlayerTextureModel textures = playerTextures.getTextures(skin, false).orElse(null);

    if (textures == null)
      return TriResult.ERR;

    npc.setSkinOwnerName(skin);
    pers.store(npc);

    findFakeNpcByModel(npc).setGameProfile(textures.toProfile());

    return TriResult.SUCC;
  }

  @Override
  public Optional<FakeNpc> getNearestNpc(Location where) {
    return this.npcs.values().stream()
      // Map each npc to their h-/v distance
      .map(npc -> (
        new Triple<>(
          npc,
          // Calculate vertical distance
          Math.abs(npc.getLoc().getY() - where.getY()),

          // Calculate horizontal distance
          Math.abs(npc.getLoc().getX() - where.getX()) + Math.abs(npc.getLoc().getZ() - where.getZ())
        )
      ))

      // Filter out npcs which are too far away
      .filter(t -> t.b() <= NEAR_NPC_MAX_DIST_V && t.c() <= NEAR_NPC_MAX_DIST_H)

      // Sort by total distance ascending
      .sorted((a, b) -> (int) Math.round((a.b() + a.c()) - (b.b() - b.c())))
      .map(Triple::a)
      .findFirst();
  }

  @Override
  public List<NpcModel> getNear(Location where, double rangeRadius) {
    // This should never be the case...
    if (where.getWorld() == null)
      throw new PersistenceException("Cannot find any near npcs if no world has been provided");

    return pers.find(
      new QueryBuilder<>(
        // Has to be in the same world
        NpcModel.class, "loc__world", EqualityOperation.EQ, where.getWorld().getName()
      )
        // X range constraint
        .and(
          new FieldQueryGroup("loc__x", EqualityOperation.GTE, where.getX() - rangeRadius)
            .and("loc__x", EqualityOperation.LTE, where.getX() + rangeRadius)
        )

        // Y range constraint
        .and(
          new FieldQueryGroup("loc__y", EqualityOperation.GTE, where.getY() - rangeRadius)
            .and("loc__y", EqualityOperation.LTE, where.getY() + rangeRadius)
        )

        // Z range constraint
        .and(
          new FieldQueryGroup("loc__z", EqualityOperation.GTE, where.getZ() - rangeRadius)
            .and("loc__z", EqualityOperation.LTE, where.getZ() + rangeRadius)
        )
    );
  }

  @Override
  public void cleanup() {
    if (intervalHandle > 0)
      Bukkit.getScheduler().cancelTask(intervalHandle);

    for (FakeNpc npc : npcs.values())
      npc.destroy();

    npcs.clear();
    npcIds.clear();
  }

  @Override
  public void initialize() {
    for (NpcModel npc : pers.list(NpcModel.class)) {
      FakeNpc fNpc = fakeNpcFromModel(npc);
      npcs.put(npc.getName().toLowerCase(), fNpc);
      npcIds.put(fNpc.getEntityId(), fNpc);
    }

    intervalHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
      for (FakeNpc npc : npcs.values())
        npc.tick();
    }, 0L, UPDATE_INTERVAL_TICKS);
  }

  @Override
  public Packet<?> modifyIncoming(UUID sender, NetworkManager nm, Packet<?> incoming) {
    // A player used an entity (left- or right click)
    if (sender != null && incoming instanceof PacketPlayInUseEntity pack) {

      try {
        Player p = Bukkit.getPlayer(sender);
        int entityId = refl.getFieldByType(pack, int.class, 0);


        FakeNpc target = npcIds.get(entityId);

        // Not a fake NPC, do nothing
        if (target == null)
          return incoming;

        // Debounce packets, ignore bursts (but still drop the packet)
        Long lastEmit = lastEventEmits.get(sender);
        if (lastEmit != null && System.currentTimeMillis() < lastEmit + EVENT_DEBOUNCE_MS)
          return null;

        // Get the first enum defined within the packet's class (is the interact type, as there's only one)
        Class<?> actionEnumC = Arrays.stream(PacketPlayInUseEntity.class.getDeclaredClasses())
          .filter(Class::isEnum)
          .findFirst()
          .orElseThrow();

        // Get the use-action interface type'd field within the packet
        Class<?> actionC = refl.findInnerClass(pack.getClass(), "EnumEntityUseAction");
        Object useAction = refl.getFieldByType(pack, actionC, 0);

        // Invoke the method of that interface which returns the action enum
        Enum<?> action = (Enum<?>) refl.findMethodByReturn(useAction.getClass(), actionEnumC)
          .orElseThrow()
          .invoke(useAction);

        // The boolean signals whether the interacting player was sneaking
        boolean isSneaking = refl.getFieldByType(pack, boolean.class, 0);

        // Create a new npc event from these parameters and decode the action
        NpcInteractEvent event = new NpcInteractEvent(
          p,
          action.ordinal() == 1 ? NpcInteraction.HIT : NpcInteraction.INTERACTED,
          isSneaking,
          target.getName()
        );

        // Fire the event synchronously
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
        this.lastEventEmits.put(sender, System.currentTimeMillis());

        // Catch this packet
        return null;
      } catch (Exception e) {
        logger.logError(e);
      }
    }

    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(UUID receiver, NetworkManager nm, Packet<?> outgoing) {
    return outgoing;
  }

  //=========================================================================//
  //                                 Listener                                //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    this.lastEventEmits.remove(e.getPlayer().getUniqueId());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Find a fake npc by it's model from cache, creates cache entry if
   * the npc could not be located
   * @param model Corresponding model
   * @return Fake npc found or created
   */
  private FakeNpc findFakeNpcByModel(NpcModel model) {
    String name = model.getName().toLowerCase();

    if (this.npcs.containsKey(name))
      return this.npcs.get(name);

    FakeNpc fn = fakeNpcFromModel(model);
    this.npcs.put(name, fn);
    this.npcIds.put(fn.getEntityId(), fn);

    return fn;
  }

  /**
   * Create a new managed fake npc from it's underlying model
   * @param model Corresponding model
   * @return New fake npc
   */
  private FakeNpc fakeNpcFromModel(NpcModel model) {
    return new FakeNpc(
      model.getLoc(),
      playerTextures.getProfileOrDefault(model.getSkinOwnerName()),
      generateEntityId(), model.getName(), npcComm, plugin
    );
  }

  /**
   * Try to generate a new, unused entity ID
   * @return Unused entity ID, -1 on errors
   */
  private int generateEntityId() {
    try {
      Class<?> eC = Entity.class;

      // Get the atomic integer field (static), which is used to
      // keep track of the last used entity id
      Field ecF =  Arrays.stream(eC.getDeclaredFields())
        .filter(f -> f.getType().equals(AtomicInteger.class))
        .findFirst()
        .orElseThrow();

      ecF.setAccessible(true);
      AtomicInteger entCounter = (AtomicInteger) ecF.get(null);

      return entCounter.incrementAndGet();
    } catch (Exception e) {
      logger.logError(e);
      return -1;
    }
  }

  /**
   * Build the selection query for a specific npc by name
   * @param name Target name
   */
  private QueryBuilder<NpcModel> buildQuery(String name) {
    return new QueryBuilder<>(
      NpcModel.class,
      "name", EqualityOperation.EQ, name
    );
  }
}
