package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.events.IMoveListener;
import me.blvckbytes.blvcksys.util.ChatButtons;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Send a teleport request to a given player.
*/
@AutoConstruct
public class TpaCommand extends APlayerCommand implements ITpaCommand, Listener {

  // TODO: Animation and sounds while teleporting

  /**
   * Represents a teleportation request
   * @param target Target player
   * @param timeoutHandle Timeout task handle
   * @param acceptPrompt Used to prompt the target to accept/deny
   */
  private record TeleportRequest(
    Player target,
    int timeoutHandle,
    ChatButtons acceptPrompt
  ) {}

  // Timeout in ticks for a pending teleport request
  private static final long REQUEST_TIMEOUT = 20 * 60;

  // Timeout in ticks for how long not to move during a teleportation
  private static final long TELEPORT_TIMEOUT = 20 * 3;

  // Currently pending requests, mapping senders to requested targets
  // where each player may have multiple requests simultaneously
  private final Map<Player, List<TeleportRequest>> pendingRequests;

  // Each Player and their teleporting task
  private final Map<Player, Integer> teleportingTasks;

  private final IMoveListener move;
  private final ChatUtil chat;

  public TpaCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IMoveListener move,
    @AutoInject ChatUtil chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "tpa",
      "Send a teleport request",
      null,
      new CommandArgument("<player>", "Request recipient")
    );

    this.pendingRequests = new HashMap<>();
    this.teleportingTasks = new HashMap<>();

    this.move = move;
    this.chat = chat;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOnlinePlayers(args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0);

    // Cannot send yourself requests
    if (target.equals(p)) {
      p.sendMessage(
        cfg.get(ConfigKey.TPA_SELF)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Doesn't have a request map entry yet
    if (!pendingRequests.containsKey(p))
      pendingRequests.put(p, new ArrayList<>());

    // Already requested this target
    if (findRequest(p, target).isPresent()) {
      p.sendMessage(
        cfg.get(ConfigKey.TPA_STILL_PENDING)
          .withPrefix()
          .withVariable("target", target.getName())
          .asScalar()
      );
      return;
    }

    // Create a timeout to let this request expire automatically
    int timeoutHandle = Bukkit.getScheduler().scheduleSyncDelayedTask(
      plugin,
      () -> expireRequest(p, target),
      REQUEST_TIMEOUT
    );

    // Create buttons to accept/deny
    ChatButtons buttons = ChatButtons.buildYesNo(
        cfg.get(ConfigKey.TPA_RECEIVED_PREFIX)
          .withPrefix()
          .withVariable("sender", p.getName())
          .asScalar(),
        plugin, cfg,

        // Yes
        () -> acceptRequest(p, target),

        // No
        () -> denyRequest(p, target)
      );

    // Register the new request
    pendingRequests.get(p).add(new TeleportRequest(target, timeoutHandle, buttons));

    // Inform sender
    p.sendMessage(
      cfg.get(ConfigKey.TPA_SENT)
        .withPrefix()
        .withVariable("target", target.getName())
        .asScalar()
    );

    // Display action screen to the target
    this.chat.sendButtons(target, buttons);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public boolean acceptRequest(Player sender, Player target) {
    Optional<TeleportRequest> req = findRequest(sender, target);

    // Request not existing
    if (req.isEmpty())
      return false;

    // Cancel a previous teleport that's still active
    Integer prevTask = this.teleportingTasks.remove(sender);
    if (prevTask != null) {
      cancelRequest(sender, target);
      Bukkit.getScheduler().cancelTask(prevTask);
    }

    // Create a teleportation task that unregisters the move listener on success,
    // where the move listener will cancel the teleportation task on move and then
    // unregister itself
    TeleportRequest tReq = req.get();
    createTeleportationTask(sender, target, tReq, cancelTeleportOnMove(sender, target, tReq));

    // Inform sender about accept
    sender.sendMessage(
      cfg.get(ConfigKey.TPA_ACCEPTED_SENDER)
        .withPrefix()
        .withVariable("target", target.getName())
        .withVariable("seconds", TELEPORT_TIMEOUT / 20)
        .asScalar()
    );

    // Inform target about accept
    target.sendMessage(
      cfg.get(ConfigKey.TPA_ACCEPTED_RECEIVER)
        .withPrefix()
        .withVariable("sender", sender.getName())
        .asScalar()
    );

    return true;
  }

  @Override
  public boolean denyRequest(Player sender, Player target) {
    Optional<TeleportRequest> req = findRequest(sender, target);

    // Request not existing
    if (req.isEmpty())
      return false;

    // Delete request
    deleteRequest(sender, req.get());

    // Inform sender about deny
    sender.sendMessage(
      cfg.get(ConfigKey.TPA_DENIED_SENDER)
        .withPrefix()
        .withVariable("target", target.getName())
        .asScalar()
    );

    // Inform target about deny
    target.sendMessage(
      cfg.get(ConfigKey.TPA_DENIED_RECEIVER)
        .withPrefix()
        .withVariable("sender", sender.getName())
        .asScalar()
    );

    return true;
  }

  @Override
  public boolean cancelRequest(Player sender, Player target) {
    Optional<TeleportRequest> req = findRequest(sender, target);

    // Request not existing
    if (req.isEmpty())
      return false;

    // Delete request
    TeleportRequest telReq = req.get();
    deleteRequest(sender, telReq);

    // Invalidate the buttons that prompted the target
    chat.removeButtons(target, telReq.acceptPrompt);

    // Inform sender about cancel
    sender.sendMessage(
      cfg.get(ConfigKey.TPA_CANCELLED_SENDER)
        .withPrefix()
        .withVariable("target", target.getName())
        .asScalar()
    );

    // Inform target about cancel
    target.sendMessage(
      cfg.get(ConfigKey.TPA_CANCELLED_RECEIVER)
        .withPrefix()
        .withVariable("sender", sender.getName())
        .asScalar()
    );

    return true;
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    deleteAllRequests(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Delete all requests where this player is involved in and notify their partners
   * @param involved Target player
   */
  private void deleteAllRequests(Player involved) {
    // Cancel all their sent requests
    List<TeleportRequest> sentRequests = pendingRequests.remove(involved);

    if (sentRequests != null) {
      for (TeleportRequest req : sentRequests) {
        // Cancel teleport timeout
        Bukkit.getScheduler().cancelTask(req.timeoutHandle);

        // Invalidate the buttons that prompted the target
        chat.removeButtons(req.target, req.acceptPrompt);

        // Inform the target
        req.target.sendMessage(
          cfg.get(ConfigKey.TPA_SENDER_QUIT)
            .withPrefix()
            .withVariable("sender", involved.getName())
            .asScalar()
        );
      }
    }

    // Cancel all requests that were targetted at the involved player
    for (Map.Entry<Player, List<TeleportRequest>> requests : pendingRequests.entrySet()) {
      // Find all requests that this player is involved in
      List<TeleportRequest> invReqs = requests.getValue()
        .stream()
        .filter(x -> x.target.equals(involved))
        .toList();

      for (TeleportRequest invReq : invReqs) {
        // Remove this request
        requests.getValue().remove(invReq);

        // Cancel teleport timeout
        Bukkit.getScheduler().cancelTask(invReq.timeoutHandle);

        // Inform the target
        requests.getKey().sendMessage(
          cfg.get(ConfigKey.TPA_RECEIVER_QUIT)
            .withPrefix()
            .withVariable("target", involved.getName())
            .asScalar()
        );

        // Cancel any teleporting task the sender of this request may be in
        Integer handle = teleportingTasks.remove(requests.getKey());
        if (handle != null)
          Bukkit.getScheduler().cancelTask(handle);
      }
    }

    // Cancel any teleporting task
    Integer handle = teleportingTasks.remove(involved);
    if (handle != null)
      Bukkit.getScheduler().cancelTask(handle);
  }

  /**
   * Create a teleportation task (timeout) to teleport the sender if they haven't
   * moved until the timeout elapsed, also unregisters the moveListener at that point
   * @param sender Sender that's not allowed to move
   * @param target Target that the sender wants to teleport to
   * @param req Teleportation request ref
   * @param moveListener Move listener runnable ref to unregister
   */
  private void createTeleportationTask(Player sender, Player target, TeleportRequest req, Runnable moveListener) {
    // Create a timeout to await non-movement in
    this.teleportingTasks.put(sender, Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      // Stop listening to moves
      move.unsubscribe(sender, moveListener);

      // Remove the finished task
      teleportingTasks.remove(sender);

      // Delete request
      deleteRequest(sender, req);

      // Teleport
      sender.teleport(target);

      // Inform sender about teleport
      sender.sendMessage(
        cfg.get(ConfigKey.TPA_TELEPORTED_SENDER)
          .withPrefix()
          .withVariable("target", target.getName())
          .asScalar()
      );

      // Inform target about teleport
      target.sendMessage(
        cfg.get(ConfigKey.TPA_TELEPORTED_RECEIVER)
          .withPrefix()
          .withVariable("sender", sender.getName())
          .asScalar()
      );
    }, TELEPORT_TIMEOUT));
  }

  /**
   * Cancel a currently active teleport if the sender moves
   * @param sender Sender that's not allowed to move
   * @param target Target that the sender wants to teleport to
   * @param req Teleportation request ref
   * @return Runnable used to register within {@link IMoveListener}
   */
  private Runnable cancelTeleportOnMove(Player sender, Player target, TeleportRequest req) {
    return this.move.subscribe(sender, new Runnable() {

      @Override
      public void run() {
        // Cancel the teleporting task
        Integer handle = teleportingTasks.remove(sender);
        if (handle != null)
          Bukkit.getScheduler().cancelTask(handle);

        // Delete request
        deleteRequest(sender, req);

        // Inform sender about cancel
        sender.sendMessage(
          cfg.get(ConfigKey.TPA_MOVED_SENDER)
            .withPrefix()
            .withVariable("target", target.getName())
            .asScalar()
        );

        // Inform target about cancel
        target.sendMessage(
          cfg.get(ConfigKey.TPA_MOVED_RECEIVER)
            .withPrefix()
            .withVariable("sender", sender.getName())
            .asScalar()
        );

        // Stop subscribing to movement
        move.unsubscribe(sender, this);
      }
    });
  }

  /**
   * Find an active request by sender and target
   * @param sender Sending player
   * @param target Target player
   * @return Optional request
   */
  private Optional<TeleportRequest> findRequest(Player sender, Player target) {
    List<TeleportRequest> requests = pendingRequests.get(sender);

    // No requests from sender exist
    if (requests == null)
      return Optional.empty();

    // Return a matching request if existing
    return requests
      .stream()
      .filter(tr -> tr.target.equals(target))
      .findFirst();
  }

  /**
   * Delete a pending request
   * @param sender Sender of the request
   * @param req Request to delete
   */
  private void deleteRequest(Player sender, TeleportRequest req) {
    List<TeleportRequest> requests = pendingRequests.get(sender);
    if (requests == null)
      return;

    // Remove this request and cancel it's timeout task
    requests.remove(req);
    Bukkit.getScheduler().cancelTask(req.timeoutHandle);
  }

  /**
   * Expire a request by deleting it and informing the participants
   * @param sender Player that has sent the request
   * @param target Player that the request was pointed towards
   */
  private void expireRequest(Player sender, Player target) {
    Optional<TeleportRequest> request = findRequest(sender, target);

    // Request not existing anymore
    if (request.isEmpty())
      return;

    // Inform the sender about expiration
    sender.sendMessage(
      cfg.get(ConfigKey.TPA_EXPIRED_SENDER)
        .withPrefix()
        .withVariable("target", target.getName())
        .asScalar()
    );

    // Inform the receiver about expiration
    target.sendMessage(
      cfg.get(ConfigKey.TPA_EXPIRED_RECEIVER)
        .withPrefix()
        .withVariable("sender", sender.getName())
        .asScalar()
    );

    deleteRequest(sender, request.get());
  }
}
