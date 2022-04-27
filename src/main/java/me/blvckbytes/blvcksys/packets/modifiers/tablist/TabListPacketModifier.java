package me.blvckbytes.blvcksys.packets.modifiers.tablist;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.events.PlayerPermissionsChangedEvent;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@AutoConstruct
public class TabListPacketModifier implements IPacketModifier, Listener, IAutoConstructed, ITabListModifier {

  private static final SimpleDateFormat df;

  static {
    df = new SimpleDateFormat("HH:mm:ss");
  }

  private final IConfig cfg;
  private final MCReflect refl;
  private final JavaPlugin plugin;

  // Known groups
  private final List<TabListGroup> groups;

  // Members per group
  private final Map<TabListGroup, List<Player>> members;

  // Created groups per player (each group has to be created once per client)
  private final Map<Player, List<TabListGroup>> createdGroups;

  // Handle of the repeating task that takes care of periodically
  // sending out tablist header and footers (for live variables)
  private int taskHandle;

  public TabListPacketModifier(
    @AutoInject IPacketInterceptor interceptor,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject JavaPlugin plugin
  ) {
    this.cfg = cfg;
    this.refl = refl;
    this.plugin = plugin;
    this.members = new HashMap<>();
    this.createdGroups = new HashMap<>();
    this.groups = new ArrayList<>();

    this.loadGroups();
    interceptor.register(this);
  }

  //=========================================================================//
  //                                Modifiers                                //
  //=========================================================================//

  @Override
  public Packet<?> modifyIncoming(Player sender, NetworkManager nm, Packet<?> incoming) {
    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(Player receiver, NetworkManager nm, Packet<?> outgoing) {
    // Override header and footer packets
    if (outgoing instanceof PacketPlayOutPlayerListHeaderFooter)
      return generateTabHeaderFooter(receiver, false);
    return outgoing;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void cleanup() {
    // Remove all groups to start over from a known state on next load
    for (Player t : Bukkit.getOnlinePlayers()) {
      // Reset the tab header and footer
      refl.sendPacket(t, generateTabHeaderFooter(t, true));

      // Remove all previously created groups
      removeAllGroups(t);
    }

    // Kill the repeating task
    Bukkit.getScheduler().cancelTask(taskHandle);
  }

  @Override
  public void initialize() {
    // Send out header and footer packets every second to keep variables up to date
    taskHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
      // Send the tab header and footer to all players
      for (Player t : Bukkit.getOnlinePlayers())
        refl.sendPacket(t, generateTabHeaderFooter(t, false));
    }, 0L, 20L);
  }

  @Override
  public Optional<TabListGroup> getPlayerGroup(Player p) {
    // Search through all groups to find the one where the player is a member
    for (Map.Entry<TabListGroup, List<Player>> group : members.entrySet()) {
      if (group.getValue().contains(p))
        return Optional.of(group.getKey());
    }

    // Player is not member of any groups
    return Optional.empty();
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onJoin(PlayerJoinEvent e) {
    // Create all currently known groups for the first time for this client
    createGroups(e.getPlayer());

    // Send out the header and footer packet
    refl.sendPacket(e.getPlayer(), generateTabHeaderFooter(e.getPlayer(), false));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Remove offline players from their group
    resetPlayerGroup(e.getPlayer());
  }

  @EventHandler
  public void onPermissionsChanged(PlayerPermissionsChangedEvent e) {
    decideDisplayGroup(e.getPlayer(), e.getActive());
  }

  //=========================================================================//
  //                                  Groups                                 //
  //=========================================================================//

  /**
   * Decide the displayed group of a player based on their meta-permissions and apply it
   * @param p Target player
   * @param permissions List of active permissions
   */
  private void decideDisplayGroup(Player p, List<String> permissions) {
    // Decide the group that will be displayed (lowest priority -> most important)
    TabListGroup displayGroup = null;
    for (String permission : permissions) {
      // Only search for group meta-permissions
      if (!permission.startsWith("group."))
        continue;

      // Get the group by it's name
      String groupName = permission.substring(permission.indexOf('.') + 1);
      Optional<TabListGroup> tg = getGroup(groupName);

      // Unknown group
      if (tg.isEmpty())
        continue;

      // Update the dispalygroup initially and for every group of higher importance
      if (displayGroup == null || displayGroup.priority() > tg.get().priority())
        displayGroup = tg.get();
    }

    // In no known group, reset back to default
    if (displayGroup == null)
      resetPlayerGroup(p);

      // Set the player's group
    else
      setPlayerGroup(displayGroup, p);
  }


  /**
   * Remove a player from their current group
   * @param p Player to remove
   */
  private void resetPlayerGroup(Player p) {
    // Remove the player from all teams
    for (Map.Entry<TabListGroup, List<Player>> team : members.entrySet()) {
      // Send out packets
      if (team.getValue().remove(p))
        broadcastGroupState(team.getKey(), p, false);
    }
  }

  /**
   * Set a player's group membership
   * @param group Target group
   * @param p Player to add to that group
   */
  private void setPlayerGroup(TabListGroup group, Player p) {
    // Remove from any previous group
    resetPlayerGroup(p);

    // Create empty list initially
    boolean isNew = false;
    if (!members.containsKey(group)) {
      members.put(group, new ArrayList<>());
      isNew = true;
    }

    // Add the member
    members.get(group).add(p);

    // This group wasn't known until now, create for all players
    if (isNew) {
      for (Player t : Bukkit.getOnlinePlayers())
        createGroup(group, members.get(group), t);
    }

    // Just update the group state for all players
    else
      broadcastGroupState(group, p, true);
  }

  /**
   * Get a group by it's name (ignores casing)
   * @param name Name of the group
   * @return Target group
   */
  private Optional<TabListGroup> getGroup(String name) {
    return groups.stream()
      .filter(currName -> name.equalsIgnoreCase(currName.groupName()))
      .findFirst();
  }

  /**
   * Load all existing groups from the config, by the corresponding prefix maps
   * Format: List of <GroupName>;<Prefix> where the order dictates the priority
   */
  private void loadGroups() {
    // Fetch a list of prefixes from config
    List<String> prefixesData = cfg.get(ConfigKey.TABLIST_PREFIXES).asList();
    for (int i = 0; i < prefixesData.size(); i++) {
      // Split CSV values
      String[] prefixData = prefixesData.get(i).split(";");

      // Malformed entry
      if (prefixData.length != 2)
        continue;

      // Add group with priority as it occurred in the config
      this.groups.add(createGroup(prefixData[0], prefixData[1], i));
    }
  }

  /**
   * Shorthand to create a tablist group by it's important parameters
   * @param name Name of the group
   * @param prefix Prefix of the group
   * @param priority Priority for tab-sorting
   * @return Created group
   */
  private TabListGroup createGroup(String name, String prefix, int priority) {
    ChatColor nameColor = ChatColor.GRAY;

    // Loop through prefix backwards
    for (int i = prefix.length() - 1; i >= 0; i--) {
      char curr = prefix.charAt(i), prev = prefix.charAt(i - 1);

      // Previous char was a color indicator
      if (prev == '§') {
        // Color not parsable
        ChatColor currColor = ChatColor.getByChar(curr);
        if (currColor == null)
          continue;

        // Save last parsable color as the name color
        nameColor = currColor;
        break;
      }
    }

    return new TabListGroup(name, prefix, "", nameColor, priority);
  }

  /**
   * Remove all known groups from a player's clients
   * @param t Packet receiver
   */
  private void removeAllGroups(Player t) {
    // Player is not yet registered
    List<TabListGroup> groups = createdGroups.remove(t);
    if (groups == null)
      return;

    // For all previously registered groups
    for (TabListGroup group : groups) {
      // Remove this group
      generateScoreboardTeam(
        group,
        TabListAction.REMOVE,
        null
      ).ifPresent(p -> refl.sendPacket(t, p));
    }
  }

  /**
   * Apply the current state of a group's memberships to all players
   * @param group Group to update
   * @param delta The change that occurred
   * @param added Whether or not the player has been added
   */
  private void broadcastGroupState(TabListGroup group, Player delta, boolean added) {
    for (Player t : Bukkit.getOnlinePlayers())
      applyGroupState(group, t, delta, added);
  }

  /**
   * Apply the current state of a group's memberships to a specific player
   * @param group Group to update
   * @param t Target player
   * @param delta The change that occurred
   * @param added Whether or not the player has been added
   */
  private void applyGroupState(TabListGroup group, Player t, Player delta, boolean added) {
    generateScoreboardTeam(
      group,
      added ? TabListAction.ADD_MEMBERS : TabListAction.REMOVE_MEMBERS,
      List.of(delta)
    ).ifPresent(p -> refl.sendPacket(t, p));
  }

  /**
   * Create a group for a player
   * @param group Group to create
   * @param members Initial group members
   * @param t Packet receiver
   */
  private void createGroup(TabListGroup group, List<Player> members, Player t) {
    generateScoreboardTeam(
      group,
      TabListAction.CREATE,
      members
    ).ifPresent(p -> {
      refl.sendPacket(t, p);

      // Remember that this client now knows this group
      if (!createdGroups.containsKey(t))
        createdGroups.put(t, new ArrayList<>());
      createdGroups.get(t).add(group);
    });
  }

  /**
   * Create all known groups for a player
   * @param t Packet receiver
   */
  private void createGroups(Player t) {
    for (Map.Entry<TabListGroup, List<Player>> group : members.entrySet())
      createGroup(group.getKey(), group.getValue(), t);
  }

  /**
   * Get a EnumChatFormat by it's color character
   * @param color Color character
   * @return Result or empty on errors
   */
  private Optional<EnumChatFormat> chatFormatFromColor(char color) {
    // Loop all enum values
    for (EnumChatFormat cf : EnumChatFormat.values()) {
      Optional<Object> colorCode = refl.getFieldByType(cf, char.class);

      // Could not get the color code of this enum entry
      if (colorCode.isEmpty())
        continue;

      // Color char matches
      if (((char) colorCode.get()) == color)
        return Optional.of(cf);
    }

    // Not found
    return Optional.empty();
  }

  /**
   * Generate a parameterized scoreboard packet form internal datatypes
   * @param group Target group
   * @param action Packet action
   * @param members List of members, can be null
   * @return Generated packet, empty on success
   */
  private Optional<Packet<?>> generateScoreboardTeam(
    TabListGroup group,
    TabListAction action,
    Collection<? extends Player> members
  ) {
    // Provide a fallback to allow for null-values
    // Null-values are used when just the team itself is modified
    if (members == null)
      members = new ArrayList<>();

    // Create a list of member names
    final List<String> memberNames = members.stream().map(Player::getName).toList();

    // Create the scoreboard team packet's inner data model
    return refl.createGarbageInstance(PacketPlayOutScoreboardTeam.b.class)
      .flatMap(b -> {

        refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage(group.groupName()), 0);
        refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage(group.prefix()), 1);
        refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage(group.suffix()), 2);
        refl.setFieldByType(b, String.class, "always", 0); // Name tag visibility: always, hideForOtherTeams, hideForOwnTeam, never
        refl.setFieldByType(b, String.class, "pushOwnTeam", 1); // Collision rule: always, pushOtherTeams, pushOwnTeam, never

        chatFormatFromColor(group.nameColor().getChar()).ifPresent(ecf -> {
          refl.setFieldByType(b, EnumChatFormat.class, ecf, 0); // Player name color
        });

        refl.setFieldByType(b, int.class, 0x00, 0); // Bit mask. 0x01: Allow friendly fire, 0x02: can see invisible players on same team.

        // Create the packet itself using all initialized parameters
        return refl.invokeConstructor(
          PacketPlayOutScoreboardTeam.class,
          group.priority() + group.groupName(), // Unique team name
          action.getMode(),     // Mode (0=create, 1=remove, 2=update, 3=add entites, 4=remove entities)
          Optional.of(b),       // Optional team (not needed for (1 | 3 | 4), I guess?)
          memberNames           // Names of all team members
        ).map(o -> (Packet<?>) o);
      });
  }

  //=========================================================================//
  //                            Header and Footer                            //
  //=========================================================================//

  /**
   * Generate a player-specific tablist header and footer packet
   * @param p Receiving player
   * @param clear Whether or not to return a cleared packet
   * @return Custom generated packet
   */
  private Packet<?> generateTabHeaderFooter(Player p, boolean clear) {
    // Return a cleared packet for resetting the header and footer
    if (clear)
      return new PacketPlayOutPlayerListHeaderFooter(
        new ChatComponentText(""), new ChatComponentText("")
      );

    Map<Pattern, String> vars = ConfigValue.makeEmpty()
      .withVariable("player", p.getName())
      .withVariable("num_online", Bukkit.getOnlinePlayers().size())
      .withVariable("num_slots", plugin.getServer().getMaxPlayers())
      .withVariable("ping", p.getPing())
      .withVariable("time", df.format(new Date()))
      .exportVariables();

    return new PacketPlayOutPlayerListHeaderFooter(
      new ChatComponentText(
        cfg.get(ConfigKey.TABLIST_HEADER)
          .withVariables(vars)
          .asScalar()
      ),
      new ChatComponentText(
        cfg.get(ConfigKey.TABLIST_FOOTER)
          .withVariables(vars)
          .asScalar()
      )
    );
  }
}