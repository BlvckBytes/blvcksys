package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.events.IChatListener;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.TimeUtil;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 05/27/2022

  Create a new survey or stop the currently active instance.
 */
@AutoConstruct
public class SurveyCommand extends APlayerCommand implements ISurveyCommand, IAutoConstructed {

  // Minimum duration of a survey in seconds
  private static final int MIN_DURATION_S = 60;

  private enum SurveyAction {
    CREATE, CANCEL
  }

  private BukkitTask taskHandle;
  private int duration;
  private String question;

  // Mapping answers to their number of votes
  private final Map<String, AtomicInteger> numVotes;

  // Mapping players to their answers
  private final Map<Player, String> votes;

  private final TimeUtil time;
  private final IChatListener chat;

  public SurveyCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject TimeUtil time,
    @AutoInject IChatListener chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "survey",
      "Create a new survey",
      PlayerPermission.COMMAND_SURVEY,
      new CommandArgument("<action>", "Action to perform"),
      new CommandArgument("[duration]", "Duration of the survey"),
      new CommandArgument("[answers]", "Comma separated answer options"),
      new CommandArgument("[question]", "Question string")
    );

    this.time = time;
    this.chat = chat;
    this.numVotes = new HashMap<>();
    this.votes = new HashMap<>();
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestEnum(args, currArg, SurveyAction.class);
    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    SurveyAction action = parseEnum(SurveyAction.class, args, 0, null);

    if (action == SurveyAction.CANCEL) {
      boolean res = endSurvey();

      // System not active
      if (!res) {
        p.sendMessage(
          cfg.get(ConfigKey.SURVEY_NONE)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      chat.broadcastMessage(
        Bukkit.getOnlinePlayers(),
        cfg.get(ConfigKey.SURVEY_CANCELLED)
          .withPrefix()
          .withVariable("executor", p.getName())
          .asScalar()
      );
      return;
    }

    // System already active
    if (taskHandle != null) {
      p.sendMessage(
        cfg.get(ConfigKey.SURVEY_EXISTS)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // CREATE

    int duration = time.parseDuration(argval(args, 1));
    String[] answers = argval(args, 2).split(",");
    String question = argvar(args, 3);

    // Too short of a duration
    if (duration < MIN_DURATION_S) {
      p.sendMessage(
        cfg.get(ConfigKey.SURVEY_INVALID_DURATION)
          .withPrefix()
          .withVariable("min_duration", time.formatDuration(MIN_DURATION_S))
          .asScalar()
      );
      return;
    }

    // Not enough answers provided
    if (answers.length < 2) {
      p.sendMessage(
        cfg.get(answers.length == 0 ? ConfigKey.SURVEY_NO_ANSWERS : ConfigKey.SURVEY_ONE_ANSWER)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    this.question = question;
    this.duration = duration;
    this.numVotes.clear();
    this.votes.clear();

    for (String answer : answers)
      this.numVotes.put(answer, new AtomicInteger(0));

    this.chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.SURVEY_LAUNCHED)
        .withPrefixes()
        .withVariable("duration", time.formatDuration(duration))
        .withVariable("creator", p.getName())
        .withVariable("question", question)
        .asScalar()
    );

    // Start the countdown
    taskHandle = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      int rem = --this.duration;

      if (rem == 0) {
        endSurvey();
        return;
      }

      // Notify about the remaining time every 30 seconds
      if (rem % 30 == 0) {
        this.chat.broadcastMessage(
          Bukkit.getOnlinePlayers(),
          cfg.get(ConfigKey.SURVEY_REMAINING)
            .withPrefixes()
            .withVariable("duration", time.formatDuration(this.duration))
            .withVariable("question", question)
            .asScalar()
        );
      }
    }, 0L, 20L);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public Set<String> getAnswers() {
    return numVotes.keySet();
  }

  @Override
  public Optional<Tuple<TriResult, String>> placeVote(Player p, String answer) {
    // Not currently active
    if (!isActive())
      return Optional.empty();

    // Answer invalid
    String answerExact = numVotes.keySet().stream().filter(a -> a.equals(answer)).findFirst().orElse(null);
    if (answerExact == null)
      return Optional.of(new Tuple<>(TriResult.ERR, null));

    // Get the previous vote
    String previousVote = votes.remove(p);

    // Undo
    if (previousVote != null)
      numVotes.get(previousVote).decrementAndGet();

    // Add vote and store answer
    numVotes.get(answerExact).incrementAndGet();
    votes.put(p, answerExact);

    return Optional.of(new Tuple<>(previousVote != null ? TriResult.EMPTY : TriResult.SUCC, answerExact));
  }

  @Override
  public boolean isActive() {
    return taskHandle != null;
  }

  @Override
  public void cleanup() {
    endSurvey();
  }

  @Override
  public void initialize() {}

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Ends the current survey and presents the results publicly
   * @return Whether a survey to end existed
   */
  private boolean endSurvey() {
    if (taskHandle == null)
      return false;

    taskHandle.cancel();
    taskHandle = null;

    chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.SURVEY_COMPLETE_HEAD)
        .withPrefixes()
        .withVariable("question", question)
        .asScalar()
    );

    int votesTotal = 0;
    for (AtomicInteger votes : numVotes.values())
      votesTotal += votes.get();

    for (Map.Entry<String, AtomicInteger> vote : numVotes.entrySet()) {
      chat.broadcastMessage(
        Bukkit.getOnlinePlayers(),
        cfg.get(ConfigKey.SURVEY_COMPLETE_ANSWERS)
          .withPrefixes()
          .withVariable("answer", vote.getKey())
          .withVariable("votes", numVotes.get(vote.getKey()))
          .withVariable("percent", Math.round((double) vote.getValue().get() / votesTotal * 100D * 100D) / 100D)
          .asScalar()
      );
    }

    chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(ConfigKey.SURVEY_COMPLETE_TAIL)
        .withPrefixes()
        .withVariable("votes_total", votesTotal)
        .asScalar()
    );

    // Reset state
    numVotes.clear();
    votes.clear();
    question = null;
    duration = -1;

    return true;
  }
}
