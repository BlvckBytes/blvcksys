package me.blvckbytes.blvcksys.persistence.transformers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.persistence.models.LocationModel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Handles transforming bukkit locations.
*/
@AutoConstruct
public class LocationTransformer implements IDataTransformer<LocationModel, Location> {

  @Override
  public Location revive(LocationModel data) {
    World world = Bukkit.getWorld(data.getWorld());

    if (world == null)
      return null;

    return new Location(
      world,
      data.getX(), data.getY(), data.getZ(),
      (float) data.getYaw(), (float) data.getPitch()
    );
  }

  @Override
  public LocationModel replace(Location data) {
    World world = data.getWorld();

    if (world == null)
      return null;

    return new LocationModel(
      data.getX(), data.getY(), data.getZ(),
      data.getYaw(), data.getPitch(),
      data.getWorld().getName()
    );
  }

  @Override
  public Class<LocationModel> getKnownClass() {
    return LocationModel.class;
  }

  @Override
  public Class<Location> getForeignClass() {
    return Location.class;
  }
}
