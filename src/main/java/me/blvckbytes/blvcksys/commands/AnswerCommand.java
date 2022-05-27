package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 05/27/2022

  Take part in a currently active survey.
 */
@AutoConstruct
public class AnswerCommand extends APlayerCommand {

  private final ISurveyCommand survey;

  public AnswerCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ISurveyCommand survey
  ) {
    super(
      plugin, logger, cfg, refl,
      "answer",
      "Take part in a survey",
      null,
      new CommandArgument("<answer>", "Your answer")
    );

    this.survey = survey;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    return suggestText(args, 0, survey.getAnswers());
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {

    // There's no active survey
    if (!survey.isActive()) {
      p.sendMessage(
        cfg.get(ConfigKey.SURVEY_NONE_ACTIVE)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    String answer = argvar(args, 0);
    Tuple<TriResult, String> res = survey.placeVote(p, answer).orElse(null);

    // Survey ended in the mean time
    if (res == null) {
      p.sendMessage(
        cfg.get(ConfigKey.SURVEY_NONE_ACTIVE)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Placed an invalid vote
    if (res.a() == TriResult.ERR) {
      p.sendMessage(
        cfg.get(ConfigKey.SURVEY_VOTE_INVALID)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Vote successfully placed or moved
    p.sendMessage(
      cfg.get(res.a() == TriResult.SUCC ? ConfigKey.SURVEY_VOTE_PLACED : ConfigKey.SURVEY_VOTE_MOVED)
        .withPrefix()
        .withVariable("answer", res.b())
        .asScalar()
    );
  }
}
