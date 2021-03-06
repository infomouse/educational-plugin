package com.jetbrains.edu.learning;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.undo.DocumentReferenceManager;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.ArrayUtil;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.checker.CheckUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderSubtaskInfo;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

public class SubtaskUtils {
  private static final Logger LOG = Logger.getInstance(SubtaskUtils.class);

  private SubtaskUtils() {
  }

  public static void switchStep(@NotNull Project project, @NotNull TaskWithSubtasks task, int toSubtaskIndex) {
    switchStep(project, task, toSubtaskIndex, true);
  }

  /***
   * @param toSubtaskIndex from 0 to subtaskNum - 1
   */
  public static void switchStep(@NotNull Project project, @NotNull TaskWithSubtasks task, int toSubtaskIndex, boolean navigateToTask) {
    if (toSubtaskIndex == task.getActiveSubtaskIndex()) {
      return;
    }
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return;
    }
    VirtualFile testDir = TaskExt.findTestDir(task, taskDir);
    if (testDir == null) return;

    int fromSubtaskIndex = task.getActiveSubtaskIndex();
    for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
      TaskFile taskFile = entry.getValue();
      VirtualFile virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir);
      if (virtualFile == null) {
        continue;
      }
      Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
      if (document == null) {
        continue;
      }
      EduDocumentListener listener = null;
      if (!FileEditorManager.getInstance(project).isFileOpen(virtualFile)) {
        listener = new EduDocumentListener(taskFile, true);
        document.addDocumentListener(listener);
      }
      updatePlaceholderTexts(document, taskFile, fromSubtaskIndex, toSubtaskIndex);
      if (listener != null) {
        document.removeDocumentListener(listener);
      }
      UndoManager.getInstance(project).nonundoableActionPerformed(DocumentReferenceManager.getInstance().create(document), false);
      EditorNotifications.getInstance(project).updateNotifications(virtualFile);
      if (EduUtils.isStudentProject(project)) {
        WolfTheProblemSolver.getInstance(project).clearProblems(virtualFile);
        taskFile.setHighlightErrors(false);
      }
    }
    transformTestFile(project, toSubtaskIndex, testDir);

    // We want to dump current tool window editor state to subtask
    // before we will switch subtask
    EduUtils.saveToolWindowTextIfNeeded(project);

    task.setActiveSubtaskIndex(toSubtaskIndex);
    updateUI(project, task, taskDir, !CCUtils.isCourseCreator(project) && navigateToTask);
    if (CCUtils.isCourseCreator(project)) {
      updateOpenedTestFiles(project, testDir, fromSubtaskIndex, toSubtaskIndex);
    }
  }

  private static void updateOpenedTestFiles(@NotNull Project project,
                                            @NotNull VirtualFile testDir,
                                            int fromTaskNumber,
                                            int toSubtaskNumber) {
    String fromSubtaskTestName = getTestFileName(project, fromTaskNumber);
    String toSubtaskTestName = getTestFileName(project, toSubtaskNumber);
    if (fromSubtaskTestName == null || toSubtaskTestName == null) {
      return;
    }
    VirtualFile fromTest = testDir.findChild(fromSubtaskTestName);
    VirtualFile toTest = testDir.findChild(toSubtaskTestName);
    if (fromTest == null || toTest == null) {
      return;
    }
    FileEditorManager editorManager = FileEditorManager.getInstance(project);
    if (editorManager.isFileOpen(fromTest)) {
      VirtualFile[] selectedFiles = editorManager.getSelectedFiles();
      boolean isSelected = ArrayUtil.contains(fromTest, selectedFiles);
      editorManager.closeFile(fromTest);
      editorManager.openFile(toTest, isSelected);
      if (!isSelected) {
        for (VirtualFile file : selectedFiles) {
          editorManager.openFile(file, true);
        }
      }
    }
  }

  private static void transformTestFile(@NotNull Project project, int toSubtaskIndex, VirtualFile testDir) {

    String subtaskTestFileName = getTestFileName(project, toSubtaskIndex);
    if (subtaskTestFileName == null) {
      return;
    }
    String nameWithoutExtension = FileUtil.getNameWithoutExtension(subtaskTestFileName);
    String extension = FileUtilRt.getExtension(subtaskTestFileName);
    VirtualFile subtaskTestFile = testDir.findChild(nameWithoutExtension + ".txt");
    if (subtaskTestFile != null) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        try {
          subtaskTestFile.rename(project, nameWithoutExtension + "." + extension);
        }
        catch (IOException e) {
          LOG.error(e);
        }
      });
    }
  }

  @Nullable
  public static String getTestFileName(@NotNull Project project, int subtaskIndex) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return null;
    }
    EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());
    if (configurator == null) {
      return null;
    }
    String defaultTestFileName = configurator.getTestFileName();
    String nameWithoutExtension = FileUtil.getNameWithoutExtension(defaultTestFileName);
    String extension = FileUtilRt.getExtension(defaultTestFileName);
    return nameWithoutExtension + EduNames.SUBTASK_MARKER + subtaskIndex + "." + extension;
  }

  public static void updateUI(@NotNull Project project, @NotNull Task task, VirtualFile taskDir, boolean navigateToTask) {
    CheckUtils.drawAllPlaceholders(project, task);
    ProjectView.getInstance(project).refresh();
    TaskDescriptionToolWindow toolWindow = EduUtils.getStudyToolWindow(project);
    if (toolWindow != null) {
        String text = EduUtils.getTaskTextFromTask(taskDir, task);
        if (text == null) {
            toolWindow.setEmptyText(project);
            return;
        }
        toolWindow.setTaskText(text, taskDir, project);
    }
    if (navigateToTask) {
      NavigationUtils.navigateToTask(project, task);
    }
  }

  private static void updatePlaceholderTexts(@NotNull Document document,
                                             @NotNull TaskFile taskFile,
                                             int fromSubtaskIndex,
                                             int toSubtaskIndex) {
    taskFile.setTrackLengths(false);
    for (AnswerPlaceholder placeholder : taskFile.getAnswerPlaceholders()) {
      placeholder.switchSubtask(document, fromSubtaskIndex, toSubtaskIndex);
    }
    taskFile.setTrackLengths(true);
  }

  public static void refreshPlaceholder(@NotNull Editor editor, @NotNull AnswerPlaceholder placeholder) {
    int prevSubtaskIndex = placeholder.getActiveSubtaskIndex() - 1;
    AnswerPlaceholderSubtaskInfo info = placeholder.getSubtaskInfos().get(prevSubtaskIndex);
    String replacementText = info != null ? info.getAnswer() : placeholder.getTaskText();
    EduUtils.replaceAnswerPlaceholder(editor.getDocument(), placeholder, placeholder.getRealLength(), replacementText);
  }
}