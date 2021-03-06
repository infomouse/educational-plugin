package com.jetbrains.edu.coursecreator.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import org.jetbrains.annotations.NotNull;

public class CCEditTaskTextAction extends ToggleAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(CCEditTaskTextAction.class);
  private static final String EDITING_MODE = "Editing Mode";

  public CCEditTaskTextAction() {
    super(EDITING_MODE, EDITING_MODE, AllIcons.Modules.Edit);
  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return false;
    }
    return StudyTaskManager.getInstance(project).getToolWindowMode() == TaskDescriptionToolWindow.StudyToolWindowMode.EDITING;
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    TaskDescriptionToolWindow window = EduUtils.getStudyToolWindow(project);
    if (window == null) {
      return;
    }
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }
    VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (virtualFile == null) {
      StudyTaskManager.getInstance(project).setTurnEditingMode(true);
      return;
    }

    Task task = EduUtils.getTaskForFile(project, virtualFile);
    if (task == null) {
      StudyTaskManager.getInstance(project).setTurnEditingMode(true);
      return;
    }
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      StudyTaskManager.getInstance(project).setTurnEditingMode(true);
      return;
    }
    VirtualFile taskTextFile = EduUtils.findTaskDescriptionVirtualFile(project, taskDir);
    if (taskTextFile == null) {
      LOG.info("Failed to find task.html");
      StudyTaskManager.getInstance(project).setTurnEditingMode(true);
      return;
    }
    Document document = FileDocumentManager.getInstance().getDocument(taskTextFile);
    if (!state) {
      if (document != null) {
        FileDocumentManager.getInstance().saveDocument(document);
        task.addTaskText(FileUtilRt.getNameWithoutExtension(taskTextFile.getName()), document.getText());
      }
      window.leaveEditingMode(project);
      return;
    }
    window.enterEditingMode(taskTextFile, project);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }

    super.update(e);
    if (CCUtils.isCourseCreator(project)) {
      Task task = EduUtils.getCurrentTask(project);
      e.getPresentation().setVisible(true);
      e.getPresentation().setEnabled(task != null);
    } else {
      e.getPresentation().setEnabledAndVisible(false);
    }
  }
}
