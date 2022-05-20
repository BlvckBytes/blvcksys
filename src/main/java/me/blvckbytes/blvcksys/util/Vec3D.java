package me.blvckbytes.blvcksys.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/20/2022

  Represents a vector in 3D space.
*/
@Getter
@Setter
@AllArgsConstructor
public class Vec3D {

  private double x, y, z;

  /**
   * Convert a bukkit location to a 3D vector
   * @param loc Location to convert
   * @return 3D vector
   */
  public static Vec3D fromLocation(Location loc) {
    return new Vec3D(loc.getX(), loc.getY(), loc.getZ());
  }

  /**
   * Subtract another vector in place
   * @param other Vector to subtract
   * @return Result
   */
  public Vec3D sub(Vec3D other) {
    this.x -= other.getX();
    this.y -= other.getY();
    this.z -= other.getZ();

    return this;
  }

  /**
   * Add another vector in place
   * @param other Vector to add
   * @return Result
   */
  public Vec3D add(Vec3D other) {
    this.x += other.getX();
    this.y += other.getY();
    this.z += other.getZ();

    return this;
  }

  /**
   * Add to the vector's coordinates in place
   * @param x X to add
   * @param y Y to add
   * @param z Z to add
   * @return Result
   */
  public Vec3D add(double x, double y, double z) {
    this.x += x;
    this.y += y;
    this.z += z;

    return this;
  }

  /**
   * Calculate the length of this vector
   */
  public double abs() {
    return Math.sqrt(
      Math.pow(this.x, 2) +
      Math.pow(this.y, 2) +
      Math.pow(this.z, 2)
    );
  }
}
