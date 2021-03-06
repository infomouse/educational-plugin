package com.jetbrains.edu.learning.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


abstract public class TaskNavigationAction extends DumbAwareActionWithShortcut {
  public TaskNavigationAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
    super(text, description, icon);
  }

  public void navigateTask(@NotNull final Project project) {
    EduEditor eduEditor = EduUtils.getSelectedStudyEditor(project);
    EduState eduState = new EduState(eduEditor);
    if (!eduState.isValid()) {
      return;
    }
    Task targetTask = getTargetTask(eduState.getTask());
    if (targetTask == null) {
      return;
    }

    NavigationUtils.navigateToTask(project, targetTask);

  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    navigateTask(project);
  }

  protected abstract Task getTargetTask(@NotNull final Task sourceTask);

  @Override
  public void update(AnActionEvent e) {
    EduUtils.updateAction(e);
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    EduEditor eduEditor = EduUtils.getSelectedStudyEditor(project);
    EduState eduState = new EduState(eduEditor);
    if (!eduState.isValid()) {
      return;
    }
    if (getTargetTask(eduState.getTask()) == null) {
      e.getPresentation().setEnabled(false);
    }
  }
}
