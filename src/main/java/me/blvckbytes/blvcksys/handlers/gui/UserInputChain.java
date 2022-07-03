package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.packets.communicators.bookeditor.IBookEditorCommunicator;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.UnsafeFunction;
import net.minecraft.util.Tuple;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/02/2022

  Handles creating input chains, which consist of multiple forms of user input
  collectors which each get registered with a corresponding name. Each input may
  go back to the previous input or forwards to the next stage. When the last stage
  is being completed, all collected values are emitted through the callback. Whenever
  a stage cancels, the whole chain is being cancelled and the main screen will
  be re-opened.
*/
public class UserInputChain {

  private final SingleChoiceGui singleChoiceGui;
  private final ChatUtil chatUtil;

  private final List<Consumer<Boolean>> stages;
  private int currStage;

  private final Map<String, Object> values;
  private final Consumer<Map<String, Object>> completion;
  private boolean isCancelled;

  private final GuiInstance<?> main;
  private GuiInstance<?> lastScreen;
  private boolean lastWasGui;

  /**
   * Create a new user input chain which will collect
   * all stage's values into a map
   * @param main Main screen which initiated this chain
   * @param completion Completion callback, providing all collected values
   * @param singleChoiceGui Single choice gui ref
   * @param chatUtil Chat utility ref
   */
  public UserInputChain(
    GuiInstance<?> main,
    Consumer<Map<String, Object>> completion,
    SingleChoiceGui singleChoiceGui,
    ChatUtil chatUtil
  ) {
    this.values = new HashMap<>();
    this.stages = new ArrayList<>();

    this.main = main;
    this.completion = completion;
    this.chatUtil = chatUtil;
    this.lastScreen = main;
    this.lastWasGui = true;
    this.singleChoiceGui = singleChoiceGui;
  }

  /**
   * Add a new chat prompt stage
   * @param field Name of the field
   * @param prompt Displayed chat prompt value
   * @param transformer Chat prompt transformer (parsing, for example)
   * @param errorMessage Error message to display when the transformer failed
   * @param skip Optional skip predicate
   */
  public UserInputChain withPrompt(
    String field,
    Function<Map<String, Object>, ConfigValue> prompt,
    UnsafeFunction<String, Object> transformer,
    @Nullable Function<String, ConfigValue> errorMessage,
    @Nullable Function<Map<String, Object>, Boolean> skip
  ) {
    stages.add(isBack -> {

      if (skip != null && skip.apply(values)) {
        nextStage();
        return;
      }

      lastWasGui = false;

      chatUtil.registerPrompt(
        main.getViewer(),
        prompt.apply(values).asScalar(),

        // Answer entered
        input -> {
          if (isCancelled) return;

          // Try to transform the value into the required format
          Object result;
          try {
            result = transformer.apply(input);
          }

          // Couldn't transform the value, input has mismatched
          catch (Exception e) {
            // Send the error message, if available
            if (errorMessage != null) {
              main.getViewer().sendMessage(
                errorMessage.apply(input).asScalar()
              );
            }

            // Reopen the main GUI and cancel the whole chain
            isCancelled = true;
            main.reopen(AnimationType.SLIDE_UP);
            return;
          }

          values.put(field, result);
          nextStage();
        },

        // Cancelled
        () -> {
          if (isCancelled) return;

          // Reopen the main GUI and cancel the whole chain
          isCancelled = true;
          main.reopen(AnimationType.SLIDE_UP);
        },

        // Back
        currStage > 1 ? () -> {
          if (isCancelled) return;
          previousStage();
        } : null
      );

      // Close the current inventory to be able to type into the chat
      if (lastScreen != null)
        lastScreen.close();
    });

    return this;
  }

  /**
   * Add a new single choice stage
   * @param gui GUI ref
   * @param field Name of the field
   * @param type Type of choice (part of the screen title)
   * @param stdProvider Provider for standard GUI items
   * @param yesButton Button to display for the YES action
   * @param noButton Button to display for the NO action
   * @param skip Optional skip predicate
   */
  public UserInputChain withYesNo(
    YesNoGui gui,
    String field,
    ConfigValue type,
    IStdGuiItemsProvider stdProvider,
    ItemStack yesButton,
    ItemStack noButton,
    @Nullable Function<Map<String, Object>, Boolean> skip
  ) {
    stages.add(isBack -> {

      if (skip != null && skip.apply(values)) {
        nextStage();
        return;
      }

      YesNoParam param = new YesNoParam(
        type.asScalar(), stdProvider, yesButton, noButton,

        // Has chosen
        (selection, selectionInst) -> {
          if (isCancelled) return;

          // Closed the inventory, cancel
          if (selection == TriResult.EMPTY) {
            if (isCancelled) return;

            // Reopen the main GUI and cancel the whole chain
            isCancelled = true;
            main.reopen(AnimationType.SLIDE_UP);
            return;
          }

          // Store a boolean value
          values.put(field, selection == TriResult.SUCC);

          lastScreen = selectionInst;
          lastWasGui = true;
          nextStage();
        },

        // Back button, reopen the last stage
        selectionInst -> {
          if (isCancelled) return;
          lastScreen = selectionInst;
          lastWasGui = true;
          previousStage();
        }
      );

      // Animate between last and current
      if (lastWasGui)
        lastScreen.switchTo(isBack ? AnimationType.SLIDE_RIGHT : AnimationType.SLIDE_LEFT, gui, param);

      // Just shift up the current GUI
      else
        gui.show(main.getViewer(), param, AnimationType.SLIDE_UP);
    });

    return this;
  }

  /**
   * Add a new single choice stage
   * @param field Name of the field
   * @param type Type of choice (part of the screen title)
   * @param stdProvider Provider for standard GUI items
   * @param representitives List of representitive items and their values
   * @param skip Optional skip predicate
   */
  public UserInputChain withChoice(
    String field,
    ConfigValue type,
    IStdGuiItemsProvider stdProvider,
    Function<Map<String, Object>, List<Tuple<Object, ItemStack>>> representitives,
    @Nullable Function<Map<String, Object>, Boolean> skip
  ) {
    return withChoice(null, null, field, type, stdProvider, representitives, skip);
  }

  /**
   * Add a new choice stage
   * @param multipleChoiceGui Multiple choice GUI ref, leave at null for single choices
   * @param field Name of the field
   * @param type Type of choice (part of the screen title)
   * @param stdProvider Provider for standard GUI items
   * @param representitives List of representitive items and their values
   * @param skip Optional skip predicate
   */
  public UserInputChain withChoice(
    @Nullable MultipleChoiceGui multipleChoiceGui,
    @Nullable Function<ItemStack, ItemStack> selectionTransform,
    String field,
    ConfigValue type,
    IStdGuiItemsProvider stdProvider,
    Function<Map<String, Object>, List<Tuple<Object, ItemStack>>> representitives,
    @Nullable Function<Map<String, Object>, Boolean> skip
  ) {
    stages.add(isBack -> {

      if (skip != null && skip.apply(values)) {
        nextStage();
        return;
      }

      // Closed
      Consumer<GuiInstance<?>> closed = selectionInst -> {
        if (isCancelled) return;

        // Reopen the main GUI and cancel the whole chain
        isCancelled = true;
        main.reopen(AnimationType.SLIDE_UP);
      };

      // Back button, reopen the last stage
      Consumer<GuiInstance<?>> back = selectionInst -> {
        if (isCancelled) return;
        lastScreen = selectionInst;
        lastWasGui = true;
        previousStage();
      };

      BiConsumer<Object, GuiInstance<?>> selected = (selection, selectionInst) -> {
        if (isCancelled) return;

        values.put(field, selection);

        lastScreen = selectionInst;
        lastWasGui = true;
        nextStage();
      };

      // Multiple choice
      if (multipleChoiceGui != null) {
        MultipleChoiceParam param = new MultipleChoiceParam(
          type.asScalar(), representitives.apply(values), stdProvider, selectionTransform,
          null, selected::accept, closed::accept, back::accept
        );

        // Animate between last and current
        if (lastWasGui)
          lastScreen.switchTo(isBack ? AnimationType.SLIDE_RIGHT : AnimationType.SLIDE_LEFT, multipleChoiceGui, param);

          // Just shift up the current GUI
        else
          multipleChoiceGui.show(main.getViewer(), param, AnimationType.SLIDE_UP);
      }

      // Single choice
      else {
        SingleChoiceParam param = new SingleChoiceParam(
          type.asScalar(), representitives.apply(values), stdProvider,
          null, selected::accept, closed::accept, back::accept
        );

        // Animate between last and current
        if (lastWasGui)
          lastScreen.switchTo(isBack ? AnimationType.SLIDE_RIGHT : AnimationType.SLIDE_LEFT, singleChoiceGui, param);

          // Just shift up the current GUI
        else
          singleChoiceGui.show(main.getViewer(), param, AnimationType.SLIDE_UP);
      }
    });

    return this;
  }

  /**
   * Add a new book editor stage (List I/O)
   * @param bookEditor Book editor ref
   * @param field Name of the field
   * @param prompt Prompt to display when handing out the book
   * @param content Initial content of the book
   */
  public UserInputChain withBookEditor(
    IBookEditorCommunicator bookEditor,
    String field,
    Function<Map<String, Object>, ConfigValue> prompt,
    Function<Map<String, Object>, List<String>> content
  ) {
    stages.add(isBack -> {
      lastWasGui = false;

      // Close the current inventory to be able to interact with the book
      if (lastScreen != null)
        lastScreen.close();

      bookEditor.initBookEditor(main.getViewer(), content.apply(values), submit -> {
        if (isCancelled) return;

        // Cancelled
        if (submit == null) {
          if (isCancelled) return;

          // Reopen the main GUI and cancel the whole chain
          isCancelled = true;
          main.reopen(AnimationType.SLIDE_UP);
          return;
        }

        values.put(field, submit);
        nextStage();
      });

      // Send the prompt
      main.getViewer().sendMessage(prompt.apply(values).asScalar());
    });

    return this;
  }

  /**
   * Start processing the input chain
   */
  public void start() {
    nextStage();
  }

  /**
   * Go to the previous stage or go back to the main screen
   * if there is no previous stage available
   */
  private void previousStage() {
    // No previous stage, cancel and reopen main
    currStage -= 2; // currstage is always leading by one
    if (currStage < 0) {
      isCancelled = true;
      main.reopen(lastWasGui ? AnimationType.SLIDE_RIGHT : AnimationType.SLIDE_UP, lastWasGui ? lastScreen : null);
      return;
    }

    // Execute the previous stage
    Consumer<Boolean> stage = stages.get(currStage++);
    stage.accept(true);
  }

  /**
   * Go to the next stage or call the completion callback if
   * all stages have been processed already
   */
  private void nextStage() {
    if (isCancelled) return;

    // No more stages remaining
    if (currStage >= stages.size()) {

      // Invoke the completion callback
      if (completion != null) {
        completion.accept(values);

        // Go back to the main screen
        main.reopen(lastWasGui ? AnimationType.SLIDE_RIGHT : AnimationType.SLIDE_UP, lastWasGui ? lastScreen : null);
      }

      return;
    }

    // Execute the next stage
    Consumer<Boolean> stage = stages.get(currStage++);
    stage.accept(false);
  }
}
