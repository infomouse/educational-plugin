package com.jetbrains.edu.learning.actions;


import com.intellij.icons.AllIcons;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreviousTaskAction extends TaskNavigationAction {
  public PreviousTaskAction() {
    super("Previous Task","Navigate to the previous task", AllIcons.Actions.Back);
  }

  public static final String ACTION_ID = "Educational.PreviousTask";
  public static final String SHORTCUT = "ctrl pressed COMMA";

  @Override
  protected Task getTargetTask(@NotNull final Task sourceTask) {
    return NavigationUtils.previousTask(sourceTask);
  }

  @NotNull
  @Override
  public String getActionId() {
    return ACTION_ID;
  }

  @Nullable
  @Override
  public String[] getShortcuts() {
    return new String[]{SHORTCUT};
  }
}