package me.blvckbytes.blvcksys.handlers;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    AnimationType type;
    long time;
  }

  // Delay in ticks between internal animation tick routine calls
  private static final long TICK_DELAY = 2;

  // Maps players to their currently active animations
  private final Map<Player, List<ActiveAnimation>> animations;

  private final JavaPlugin plugin;
  private int tickHandle;

  public AnimationHandler(
    @AutoInject JavaPlugin plugin
  ) {
    this.plugin = plugin;
    this.tickHandle = -1;

    this.animations = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void startAnimation(Player target, AnimationType animation) {
    // Create an empty list initially
    if (!this.animations.containsKey(target))
      this.animations.put(target, new ArrayList<>());

    // Add this new animation and start the time out at zero
    this.animations.get(target).add(new ActiveAnimation(animation, 0));
  }

  @Override
  public boolean stopAnimation(Player target, AnimationType animation) {
    // No animations for this player
    if (!this.animations.containsKey(target))
      return false;

    // Remove all animations of this type
    this.animations.get(target).removeIf(anim -> anim.type.equals(animation));
    return true;
  }

  @Override
  public boolean stopAllAnimations(Player target) {
    List<ActiveAnimation> animations = this.animations.remove(target);
    return animations != null && animations.size() > 0;
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
    animations.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Tick all animations for all players
   */
  private void tick() {
    for (Map.Entry<Player, List<ActiveAnimation>> playerAnimations : animations.entrySet())
      for (ActiveAnimation animation : playerAnimations.getValue())
        tickAnimation(playerAnimations.getKey(), animation);
  }

  /**
   * Tick an animation for a specific player
   * @param p Target player
   * @param animation Animation that's playing
   */
  private void tickAnimation(Player p, ActiveAnimation animation) {
    // Decide on the actual processor function
    if (animation.type.equals(AnimationType.ROTATING_CONE))
      tickROTATING_CONE(p, animation);

    // Increase the time tracking variable
    animation.time++;
  }

  ///////////////////////////////// ROTATING_CONE ////////////////////////////////////

  private void tickROTATING_CONE(Player p, ActiveAnimation animation) {
    Location a = p.getEyeLocation().add(0, 0.8, 0); // Head of the cone (a bit above the player's head)
    Location b = p.getLocation().add(0, 0.1, 0);    // Tail of the cone (a bit above the player's feet, so it's not clamped by the ground)

    int r = 1;                       // Radius of the cone's flat bottom
    double windingPeriods = 0.55;    // How often to wind around while travelling from tail to head
    int numSpirals = 4;              // How many spirals to wind with an even distance from each other
    double vertDist = 0.038;         // Distance between pixels vertically
    double degPerSec = 110.0;        // Degrees per second of rotation speed
    double pixelSize = 0.55;         // Size of a pixel (1 = default)

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

    // Draw all pixels
    for (Vector pixel : pixels)
      p.getWorld().spawnParticle(Particle.REDSTONE, pixel.getX(), pixel.getY(), pixel.getZ(), 1, new Particle.DustOptions(Color.PURPLE, (float) pixelSize));

  }
}
