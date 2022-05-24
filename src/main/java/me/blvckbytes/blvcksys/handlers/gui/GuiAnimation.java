package me.blvckbytes.blvcksys.handlers.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/23/2022

  Draws animations frame by frame, where usually the GUI is locked while
  drawing frames and the animator just moves raw items around, without accounting
  for any events, pagination or the like. There's always a transition from one
  inventory to another, but if the destination is null, the source becomes the
  destination and the source becomes a null-filled array. This way, both cases can
  be animated using the same routines.
*/
public class GuiAnimation {

  // How many ticks each animation frame may take up
  private static final long TICKS_PER_FRAME = 1;

  private final AnimationType animation;
  private final JavaPlugin plugin;
  private final int numFrames, numRows;
  private final Inventory from, to;
  private final Runnable done, ready;
  private final ItemStack[] fromContents, toContents;
  private final AtomicBoolean fastForwarded;

  private int currFrame;

  /**
   * Create a new animation for a GUI
   * @param plugin JavaPlugin ref, used for scheduling
   * @param animation Animation to play
   * @param from Inventory to animate from
   * @param to Inventory to animate to (for transitions, optional)
   * @param ready Ready callback, signals that the GUI may be presented by now
   * @param done Completion callback, signals that the animation is complete
   */
  public GuiAnimation(
    JavaPlugin plugin,
    AnimationType animation,
    Inventory from,
    @Nullable Inventory to,
    Runnable ready,
    Runnable done
  ) {
    this.plugin = plugin;
    this.ready = ready;
    this.done = done;
    this.animation = animation;
    this.from = from;
    this.to = to == null ? from : to;

    // No destination provided, make from the destination
    // and have from as an empty inventory
    if (to == null) {
      toContents = from.getContents().clone();
      fromContents = new ItemStack[from.getSize()];
    }
    else {
      this.fromContents = from.getContents().clone();
      this.toContents = to.getContents().clone();
    }

    this.numRows = from.getSize() / 9;
    this.numFrames = getNumFrames();
    this.fastForwarded = new AtomicBoolean(false);

    play();
  }

  /**
   * Plays the current frame, also used to bootstrap playing
   */
  private void play() {
    // Animation has been quit manually already
    if (this.fastForwarded.get())
      return;

    // Cannot transition inventories unequal in size
    if (to != null && from.getSize() != to.getSize()) {
      currFrame = numFrames;
      ready.run();
      done.run();
      return;
    }

    // Copy over the previous items before the first frame plays
    if (currFrame == 0) {
      // But only if there's a transition
      if (to != null) {
        for (int i = 0; i < from.getSize(); i++)
          to.setItem(i, from.getItem(i));
      }

      ready.run();
    }

    animate();
    nextFrame();
  }

  /**
   * Perform the current frame's animation
   */
  private void animate() {
    switch (animation) {
      // Drawing columns
      case SLIDE_RIGHT, SLIDE_LEFT -> {

        for (int drawCol = 0; drawCol < 9; drawCol++) {
          ItemStack[] origin;
          int readCol;

          if (animation == AnimationType.SLIDE_LEFT) {
            if (drawCol < (numFrames - currFrame - 1)) {
              origin = fromContents;
              readCol = drawCol + currFrame + 1;
            } else {
              origin = toContents;
              readCol = drawCol - (8 - currFrame);
            }
          }

          else {
            if (drawCol > currFrame) {
              origin = fromContents;
              readCol = drawCol - currFrame - 1;
            }
            else {
              origin = toContents;
              readCol = 8 - currFrame + drawCol;
            }
          }

          for (int i = 0; i < numRows * 9; i += 9)
            getWriting().setItem(drawCol + i, origin[readCol + i]);
        }
      }

      // Drawing rows
      case SLIDE_DOWN, SLIDE_UP -> {
        for (int drawRow = 0; drawRow < numRows; drawRow++) {
          ItemStack[] origin;
          int readRow;

          if (animation == AnimationType.SLIDE_DOWN) {
            if (drawRow > currFrame) {
              origin = fromContents;
              readRow = drawRow - (currFrame + 1);
            } else {
              origin = toContents;
              readRow = drawRow + (numRows - currFrame - 1);
            }
          }

          else {
            if (drawRow < (numFrames - currFrame - 1)) {
              origin = fromContents;
              readRow = drawRow + (currFrame + 1);
            } else {
              origin = toContents;
              readRow = drawRow - (numRows - currFrame - 1);
            }
          }

          for (int i = 0; i < 9; i++)
            getWriting().setItem(drawRow * 9 + i, origin[readRow * 9 + i]);
        }
      }
    }
  }

  /**
   * Invoke playing the next frame by calling {@link #play()} after the
   * ticks per frame have elapsed, or call the callback if there's no next frame
   */
  private void nextFrame() {
    // While there are still frames left and the inv is open, invoke another frame timer
    if (++currFrame < numFrames && getWriting().getViewers().size() > 0)
      Bukkit.getScheduler().runTaskLater(plugin, this::play, TICKS_PER_FRAME);

      // Done, call the callback
    else
      done.run();
  }

  /**
   * Get the inventory that's being written to
   */
  private Inventory getWriting() {
    return to != null ? to : from;
  }

  /**
   * Get the number of frames this animation will take
   */
  private int getNumFrames() {
    return switch (animation) {
      // Bottom and top will both take as many frames as there are rows
      case SLIDE_UP, SLIDE_DOWN -> from.getSize() / 9;

      // Left and right take as many frames as there are horizontal slots
      case SLIDE_RIGHT, SLIDE_LEFT -> 9;
    };
  }

  /**
   * Fast forwards an animation by jumping to it's last frame instantly
   */
  public void fastForward() {
    // No frames left, animation is already over
    if (currFrame >= numFrames)
      return;

    // Directly copy the contents (last frame in all cases)
    for (int i = 0; i < Math.min(fromContents.length, toContents.length); i++)
      to.setItem(i, toContents[i]);

    done.run();
  }
}
