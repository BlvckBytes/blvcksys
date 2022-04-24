package me.blvckbytes.blvcksys.managers;

import me.blvckbytes.blvcksys.packets.modifiers.tablist.TabListGroup;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

@AutoConstruct
public class GroupManager implements IGroupManager {

  // Mapping managed groups to the corresponding tab group instance
  private final Map<Group, TabListGroup> tabGroups;

  // Mapping managed groups to their members
  private final Map<Group, List<Player>> memberships;

  // List of managed groups
  private final List<Group> groups;

  private final ITabGroupManager tab;

  public GroupManager(
    @AutoInject ITabGroupManager tab
  ) {
    this.tab = tab;
    this.groups = new ArrayList<>();
    this.memberships = new HashMap<>();

    // Added statically for testing purposes
    groups.add(new Group("Admin", "§cAdmin §8❘ §c", 0));
    groups.add(new Group("Spieler", "§bSpieler §8❘ §b", 1));

    // Create tab groups from group list
    this.tabGroups = this.groups
      .stream()
      .collect(Collectors.toMap(g -> g, this::convertGroup));
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  @Override
  public List<Group> getGroups() {
    return Collections.unmodifiableList(this.groups);
  }

  @Override
  public Group getGroup(String name) {
    return this.groups
      .stream()
      .filter(g -> g.name().equalsIgnoreCase(name))
      .findFirst()
      .orElse(null);
  }

  @Override
  public void createGroup(Group group) {
    this.groups.add(group);
    this.tabGroups.put(group, convertGroup(group));
  }

  @Override
  public boolean addToGroup(Player p, Group group) {
    // Group has no members yet
    if (!memberships.containsKey(group))
      memberships.put(group, new ArrayList<>());

    // Already a member
    List<Player> members = memberships.get(group);
    if (members.contains(p))
      return false;

    // Add member
    memberships.get(group).add(p);

    // Delta occurred, update tablist
    this.decideTabListGroup(p);
    return true;
  }

  @Override
  public boolean removeFromGroup(Player p, Group group) {
    // Group has no members yet
    if (!this.memberships.containsKey(group))
      return false;

    // Remove member
    boolean removed = memberships.get(group).remove(p);

    // Delta occurred, update tablist
    if (removed)
      this.decideTabListGroup(p);

    return removed;
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  private void decideTabListGroup(Player p) {
    Group decided = null;

    // Loop all groups and their members
    for(Map.Entry<Group, List<Player>> members : memberships.entrySet()) {
      // Not a member of this group
      if (!members.getValue().contains(p))
        continue;

      // Set the group if either no group has been decided yet or
      // the current group has a higher priority
      Group curr = members.getKey();
      if (decided == null || decided.priority() > curr.priority())
        decided = curr;
    }

    // In no group, leave blank
    if (decided == null)
      tab.resetPlayerGroup(p);

    // In a group, set
    else {
      // Check if the tab list group exists
      if (!tabGroups.containsKey(decided))
        tabGroups.put(decided, convertGroup(decided));

      // Apply to the player
      tab.setPlayerGroup(tabGroups.get(decided), p);
    }
  }

  private TabListGroup convertGroup(Group group) {
    ChatColor nameColor = ChatColor.GRAY;
    String pref = group.tabPrefix();

    // Loop through prefix backwards
    for (int i = pref.length() - 1; i >= 0; i--) {
      char curr = pref.charAt(i), prev = pref.charAt(i - 1);

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

    // Create new tab list group from the parameters
    // Leaving the suffix blank for now, as I haven't jet decided on how to
    // handle suffixes (and if I need them)
    return new TabListGroup(group.name(), pref, "", nameColor, group.priority());
  }
}
