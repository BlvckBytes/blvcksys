package me.blvckbytes.blvcksys.handlers;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/01/2022

  Start and stop implemented animations for specific players.
*/
@AutoConstruct
public class AnimationHandler implements IAnimationHandler, Listener, IAutoConstructed {

  /*
   * Represents an active animation that's playing for a specific player
   *
   * type Animation that's playing
   * time Time variable, increases by one each tick and starts out as zero
   */
  @AllArgsConstructor
  private static class ActiveAnimation {
    // Holder of this location which specifies a "live location"
    // When null, the Location property will be used
    @Nullable Player holder;

    // Statically specified location
    // When null, the location of the holder has to be provided
    @Nullable Location loc;

    // What player should receive this animation, null means it's played
    // directly on the world of the location
    @Nullable List<Player> receivers;

    // Type of animation
    AnimationType type;

    // Parameter passed to this animation instance
    @Nullable Object parameter;

    // Current relative time
    long time;
  }

  // Delay in ticks between internal animation tick routine calls
  private static final long TICK_DELAY = 1;

  // Maps players to their currently active animations
  private final List<ActiveAnimation> animations;

  private final JavaPlugin plugin;
  private int tickHandle;

  public AnimationHandler(
    @AutoInject JavaPlugin plugin
  ) {
    this.plugin = plugin;
    this.tickHandle = -1;

    this.animations = new ArrayList<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void startAnimation(Player target, List<Player> receicers, AnimationType animation, @Nullable Object parameter) {
    this.animations.add(new ActiveAnimation(target, null, receicers, animation, parameter, 0));
  }

  @Override
  public void startAnimation(Location loc, List<Player> receicers, AnimationType animation, @Nullable Object parameter) {
    this.animations.add(new ActiveAnimation(null, loc, receicers, animation, parameter, 0));
  }

  @Override
  public boolean stopAnimation(Player target, AnimationType animation) {
    return this.animations.removeIf(anim -> target.equals(anim.holder) && anim.type.equals(animation));
  }

  @Override
  public boolean stopAnimation(Location loc, AnimationType animation) {
    return this.animations.removeIf(anim -> loc.equals(anim.loc) && anim.type.equals(animation));
  }

  @Override
  public boolean stopAllAnimations(Player target) {
    return this.animations.removeIf(anim -> target.equals(anim.holder));
  }

  @Override
  public void cleanup() {
    // Cancel the internal tick loop task
    if (this.tickHandle > 0)
      Bukkit.getScheduler().cancelTask(this.tickHandle);
  }

  @Override
  public void initialize() {
    // Start the internal ticking task
    this.tickHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 0L, TICK_DELAY);
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  public void onQuit(PlayerQuitEvent e) {
    // Stop all animations for this player
    animations.removeIf(anim -> e.getPlayer().equals(anim.holder));
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Tick all animations for all players
   */
  private void tick() {
    for (ActiveAnimation animation : animations)
      tickAnimation(animation);
  }

  /**
   * Tick an animation by increasing it's relative time
   *
   * @param animation Animation that's playing
   */
  private void tickAnimation(ActiveAnimation animation) {
    // Get the destination location and world
    Location loc = getLocation(animation).orElse(null);
    if (loc == null || loc.getWorld() == null)
      return;

    // Decide on the actual processor function
    if (animation.type.equals(AnimationType.PURPLE_ROTATING_CONE))
      tickROTATING_CONE(animation, loc, loc.getWorld());
    else if (animation.type.equals(AnimationType.ORANGE_DOUBLE_HELIX))
      tickDOUBLE_HELIX(animation, loc, loc.getWorld());

    // Increase the time tracking variable
    animation.time++;
  }

  /**
   * Get an animations location, accounting for priorities
   *
   * @param animation Animation in question
   * @return Location to play at
   */
  private Optional<Location> getLocation(ActiveAnimation animation) {
    if (animation.holder != null)
      return Optional.of(animation.holder.getLocation());
    if (animation.loc != null)
      return Optional.of(animation.loc);
    return Optional.empty();
  }

  /**
   * Draw a frame of an animation while accounting for the list of receiving players
   *
   * @param animation Animation handle
   * @param pixels    Pixels to draw
   * @param w         World to animate in
   */
  private void drawFrame(ActiveAnimation animation, List<Vector> pixels, World w) {
    for (Vector pixel : pixels) {
      // Play for all players - on the world itself
      if (animation.receivers == null) {
        w.spawnParticle(animation.type.getParticle(), pixel.getX(), pixel.getY(), pixel.getZ(), 1, animation.type.getOptions());
        continue;
      }

      // Play on a per-player basis, using only the receivers
      for (Player receiver : animation.receivers)
        receiver.spawnParticle(animation.type.getParticle(), pixel.getX(), pixel.getY(), pixel.getZ(), 1, animation.type.getOptions());
    }
  }

  ///////////////////////////////// ROTATING_CONE ////////////////////////////////////

  private void tickROTATING_CONE(ActiveAnimation animation, Location loc, World w) {
    Location a = loc.clone().add(0, 2.8, 0);    // Head of the cone (a bit above the player's head)
    Location b = loc.clone().add(0, 0.1, 0);    // Tail of the cone (a bit above the player's feet, so it's not clamped by the ground)

    int r = 1;                       // Radius of the cone's flat bottom
    double windingPeriods = 0.55;    // How often to wind around while travelling from tail to head
    int numSpirals = 4;              // How many spirals to wind with an even distance from each other
    double vertDist = 0.038;         // Distance between pixels vertically
    double degPerSec = 110.0;        // Degrees per second of rotation speed

    List<Vector> pixels = new ArrayList<>();

    // Phase difference between spirals (space evenly)
    double phaseDiff = (2 * Math.PI) / numSpirals;

    // One increment in animation.time corresponds to TICK_DELAY ticks, one tick corresponds to 1/20 seconds
    double elapsedSeconds = animation.time / 20.0F * TICK_DELAY;

    // Elapsed degrees are elapsedSeconds (1 deg/sec) times degPerSec
    // wrapped around 360 (0-359)
    double elapsedDegrees = (elapsedSeconds % 360) * degPerSec;

    // Dynamic phase shift, ranging from 0 to 2PI based on the current time.
    double dynamicPhaseShift = elapsedDegrees / 180F * Math.PI;

    // Calculate the total delta in Y the loop will travel
    double deltaY = a.getY() - b.getY();

    // Travel from bottom to top
    for (double y = b.getY(); y <= a.getY(); y += vertDist) {

      // Calculate the relative Y change in reference to the bottom
      double relY = b.getY() - y;

      // Calculate the amount the loop travelled (0 to 1) and it's complementary
      double amountTravelled = Math.abs(relY / deltaY);
      double amountTravelledComp = (1 - amountTravelled);

      // Create all n spirals at each y-iter
      for (int i = 0; i < numSpirals; i++) {

        // Static phase shift of the current spiral
        // Basically increasing as i increases, and thus ranges
        // from 0 to to ((numSpirals - 1) / numSpirals) * 2PI.
        // This will have a gap between spirals of phaseDiff, and
        // the first spiral will start at 0 and the last at 360 - phaseDiff,
        // so first and last won't overlap.
        double staticPhaseShift = phaseDiff * i;

        // The total phase shift is the dynamic phase shift (time) + the static phase shift (offset between spirals)
        double phaseShift = dynamicPhaseShift + staticPhaseShift;

        // Calculate the input angle to the trigonometric functions
        // amountTravelled will range from 0 to 1 as we go up from the bottom
        // multiply that by 2PI will cause one full rotation, then
        // mulitply that by the number of windings to get the resulting desired frequency
        double phi = amountTravelled * 2 * Math.PI * windingPeriods + phaseShift;

        // Call the trig functions and scale them up to the desired radius
        // The cone should get narrower as we go up the top, thus also multiply by the
        // inverse of amountTravelled, so that the top will collapse into a single point
        double xAdd = Math.cos(phi) * r * amountTravelledComp, zAdd = Math.sin(phi) * r * amountTravelledComp;

        // Add the resulting pixel to the list of pixels to draw
        pixels.add(new Vector(b.getX() + xAdd, y, b.getZ() + zAdd));
      }
    }

    drawFrame(animation, pixels, w);
  }

  ////////////////////////////////// DOUBLE_HELIX /////////////////////////////////////

  private void tickDOUBLE_HELIX(ActiveAnimation animation, Location loc, World w) {
    List<Vector> pixels = new ArrayList<>();

    if (animation.parameter == null || (!(animation.parameter instanceof DoubleHelixParameter param)))
      return;

    double pixelDist = 0.050; // Distance between pixels

    // As vectors are in unit blocks/tick, multiply by the elapsed ticks
    long elapsedTicks = animation.time * TICK_DELAY;
    Vector v = param.velocity().clone().multiply(elapsedTicks);

    // One step should advance the vector length by pixelDist
    // This means that step * len should be pixelDist
    // Which in return means that step = pixelDist / len
    double length = v.length();
    double step = pixelDist / length;

    // Walk the vector v
    for (double i = 0; i <= 1; i += step) {
      // Get the current position partways into v within the world as a vector
      Vector currPos = loc.clone().add(v.clone().multiply(i)).toVector();

      // The following section is creating a vector which is at an right angle
      // relative to the main vector v by first of all eliminating one of the three
      // axis by dividing by the smallest coordinate (skipping /0).
      // Vectors are at a right angle, when their dot product is zero, which means
      // that ax*bx + ay*by + az*bz = 0. If one component already equals zero, there
      // are only two more to solve for. Imagine x went to zero: ay*by + az*bz = 0.
      // If a is given and b is searched for, just swap a's x and y in place of b's
      // and negate one (the bigger) of the two, resulting in (when ay > az):
      // ay*az + az*(-ay) = ay*az - ay*az = 0. This is true, and thus one of the
      // infinite vectors which is at a right angle to v. This vector can now be
      // normalized, scaled and rotated about v to create the helix's pattern.

      // Find the smallest coordinate and divide the whole vector by it
      double[] cords = { v.getX(), v.getY(), v.getZ() };
      int smallest = findSmallestIndex(cords);
      Vector h = v.clone().multiply(1.0 / (cords[smallest] == 0 ? 1 : cords[smallest]));

      // Find the two remaining axies which are not zeroed out:
      // 0 1 2
      // x y z
      // 0 y z  1 2
      // x 0 z  0 2
      // x y 0  0 1
      int a = smallest == 0 ? 1 : 0;
      int b = smallest == 0 ? 2 : (smallest == 1 ? 2 : 1);

      // Swap a and b and negate the one that's bigger
      double aVal = cords[a], bVal = cords[b];
      boolean negateA = Math.abs(aVal) > Math.abs(bVal);
      setAxisByIndex(h, a, negateA ? cords[b] : -cords[b]);
      setAxisByIndex(h, b, negateA ? -cords[a] : cords[a]);

      // Turn it into a vector of that direction and length r
      h.normalize().multiply(param.radius());

      // Travelled distance "modulo" blocks per winding
      double dist = length * i;
      while (dist > param.blocksPerWinding())
        dist -= param.blocksPerWinding();

      // 0..1 of the winding period, scale to a full rotation angle
      double period = dist / param.blocksPerWinding();
      double phi = 2 * Math.PI * period;

      // Add a pixel for the vector added to the current position at phi, and one at 180deg phase shift phi + PI
      pixels.add(currPos.clone().add(rotateAbout(h, v.clone().normalize(), phi)));
      pixels.add(currPos.clone().add(rotateAbout(h, v.clone().normalize(), phi + Math.PI)));
    }

    drawFrame(animation, pixels, w);
  }

  /**
   * Rotates the vector v about the vector about by an angle of phi
   * @param v Vector to rotate
   * @param about Vector to use as an axis to rotate about
   * @param phi Angle in radians
   * @return Rotated input vector
   */
  private Vector rotateAbout(Vector v, Vector about, double phi) {
    // Thank you so much Euler and Rodrigues, great work!
    // https://en.wikipedia.org/wiki/Rodrigues%27_rotation_formula
    return v.clone().multiply(Math.cos(phi))
      .add(about.clone().crossProduct(v).multiply(Math.sin(phi)))
      .add(about.multiply(about.dot(v)).multiply(1 - Math.cos(phi)));
  }

  /**
   * Set a vector's axis by it's index, where x=0, y=1 and z=2
   * @param v Vector to manipulate
   * @param index Index of the target axis
   * @param value Value to set
   */
  private void setAxisByIndex(Vector v, int index, double value) {
    switch (index) {
      case 0 -> v.setX(value);
      case 1 -> v.setY(value);
      case 2 -> v.setZ(value);
      default -> throw new IllegalArgumentException("Index " + index + " out of range");
    }
  }

  /**
   * Finds the index of the smallest number (absolute) within an array of elements
   * @param elements Array of elements
   * @return Index of the smallest number
   */
  private int findSmallestIndex(double[] elements) {
    int j;

    // Loop all items in the array
    // Initially assumes the first to be the smallest
    for (j = 0; j < elements.length; j++) {

      // Assume j to be the smallest
      boolean smallest = true;

      // Loop all items but j
      for (int k = 0; k < elements.length; k++) {
        if (k == j)
          continue;

        // Item j is smaller than or equals to item k, smallest holds
        if (Math.abs(elements[k]) >= Math.abs(elements[j]))
          continue;

        // Bigger, not the smallest, stop searching
        smallest = false;
        break;
      }

      // j was the smallest, stop
      if (smallest)
        break;
    }

    // Last item that broke the outer loop was the smallest
    return j;
  }
}
