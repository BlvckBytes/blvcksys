package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.ImageFrameModel;
import me.blvckbytes.blvcksys.persistence.models.ImageFrameType;
import net.minecraft.util.Tuple;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.ItemFrame;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/12/2022

  Public interfaces which the image frame handler provides to other consumers.
 */
public interface IImageFrameHandler {

  /**
   * Find all nearby item frame groups within a given radius
   * @param loc Location to search at
   * @param radius Radius to search in
   * @return List of item frame groups within that radius, paired with their persistent model
   */
  List<Tuple<ImageFrameModel, ItemFrameGroup>> findNearbyGroups(Location loc, double radius);

  /**
   * Find a nearby image frame using a location
   * @param loc Target location
   * @return Frame near this location or null if there is no frame
   */
  ItemFrame findNearbyFrame(Location loc);

  /**
   * Get a frame's containing group
   * @param member Member frame of the group
   * @return Group or null if the frame is not a member of any groups
   */
  ItemFrameGroup findGroupByMember(ItemFrame member);

  /**
   * Create a new image frame group from one member-frame
   * @param creator Creator of this group
   * @param name Name of the new group
   * @param loc Location of a member-frame
   * @param type Type of this group
   * @param resource Optional resource (url, path, ...)
   * @return True on success, false if this name is already taken
   */
  boolean createGroup(
    OfflinePlayer creator,
    String name,
    Location loc,
    ImageFrameType type,
    @Nullable String resource
  ) throws PersistenceException;

  /**
   * Delete an existing group by it's name
   * @param name Name of the target group
   * @return True on success, false if there was no group with this name
   */
  boolean deleteGroup(String name) throws PersistenceException;
}
