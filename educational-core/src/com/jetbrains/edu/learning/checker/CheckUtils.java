package com.jetbrains.edu.learning.checker;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.ui.TestResultsToolWindowFactory;
import com.jetbrains.edu.learning.ui.TestResultsToolWindowFactoryKt;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class CheckUtils {
  private static final Logger LOG = Logger.getInstance(CheckUtils.class);

  private CheckUtils() {
  }

  public static void drawAllPlaceholders(@NotNull final Project project, @NotNull final Task task) {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return;
    }
    for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
      String name = entry.getKey();
      TaskFile taskFile = entry.getValue();
      VirtualFile virtualFile = taskDir.findFileByRelativePath(name);
      if (virtualFile == null) {
        continue;
      }
      FileEditor fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);
      if (fileEditor instanceof EduEditor) {
        EduEditor eduEditor = (EduEditor)fileEditor;
        EduUtils.drawAllAnswerPlaceholders(eduEditor.getEditor(), taskFile);
      }
    }
  }

  public static void navigateToFailedPlaceholder(@NotNull final EduState eduState,
                                                 @NotNull final Task task,
                                                 @NotNull final VirtualFile taskDir,
                                                 @NotNull final Project project) {
    TaskFile selectedTaskFile = eduState.getTaskFile();
    Editor editor = eduState.getEditor();
    TaskFile taskFileToNavigate = selectedTaskFile;
    VirtualFile fileToNavigate = eduState.getVirtualFile();
    final StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    if (!studyTaskManager.hasFailedAnswerPlaceholders(selectedTaskFile)) {
      for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
        String name = entry.getKey();
        TaskFile taskFile = entry.getValue();
        if (studyTaskManager.hasFailedAnswerPlaceholders(taskFile)) {
          taskFileToNavigate = taskFile;
          VirtualFile virtualFile = taskDir.findFileByRelativePath(name);
          if (virtualFile == null) {
            continue;
          }
          FileEditor fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);
          if (fileEditor instanceof EduEditor) {
            EduEditor eduEditor = (EduEditor)fileEditor;
            editor = eduEditor.getEditor();
          }
          fileToNavigate = virtualFile;
          break;
        }
      }
    }
    if (fileToNavigate != null) {
      FileEditorManager.getInstance(project).openFile(fileToNavigate, true);
    }
    final Editor editorToNavigate = editor;
    ApplicationManager.getApplication().invokeLater(
      () -> IdeFocusManager.getInstance(project).requestFocus(editorToNavigate.getContentComponent(), true));

    NavigationUtils.navigateToFirstFailedAnswerPlaceholder(editor, taskFileToNavigate);
  }


  public static void showTestResultPopUp(@NotNull final String text, Color color, @NotNull final Project project) {
    BalloonBuilder balloonBuilder =
      JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(text, null, color, null);
    final Balloon balloon = balloonBuilder.createBalloon();
    EduUtils.showCheckPopUp(project, balloon);
  }


  public static void flushWindows(@NotNull final Task task, @NotNull final VirtualFile taskDir) {
    for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
      String name = entry.getKey();
      TaskFile taskFile = entry.getValue();
      VirtualFile virtualFile = taskDir.findFileByRelativePath(name);
      if (virtualFile == null) {
        continue;
      }
      EduUtils.flushWindows(taskFile, virtualFile);
    }
  }

  public static void showTestResultsToolWindow(@NotNull final Project project, @NotNull final String message) {
    ApplicationManager.getApplication().invokeLater(() -> {
      final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
      ToolWindow window = toolWindowManager.getToolWindow(TestResultsToolWindowFactoryKt.ID);
      if (window == null) {
        toolWindowManager.registerToolWindow(TestResultsToolWindowFactoryKt.ID, true, ToolWindowAnchor.BOTTOM);
        window = toolWindowManager.getToolWindow(TestResultsToolWindowFactoryKt.ID);
        new TestResultsToolWindowFactory().createToolWindowContent(project, window);
      }

      final Content[] contents = window.getContentManager().getContents();
      for (Content content : contents) {
        final JComponent component = content.getComponent();
        if (component instanceof ConsoleViewImpl) {
          ((ConsoleViewImpl)component).clear();
          ((ConsoleViewImpl)component).print(message, ConsoleViewContentType.ERROR_OUTPUT);
          window.setAvailable(true,null);
          window.show(null);
        }
      }
    });
  }

  public static TestsOutputParser.TestsOutput getTestOutput(@NotNull Process testProcess,
                                                            @NotNull String commandLine,
                                                            boolean isAdaptive) {
    final CapturingProcessHandler handler = new CapturingProcessHandler(testProcess, null, commandLine);
    final ProcessOutput output = ProgressManager.getInstance().hasProgressIndicator() ? handler
      .runProcessWithProgressIndicator(ProgressManager.getInstance().getProgressIndicator()) :
                                 handler.runProcess();
    final TestsOutputParser.TestsOutput testsOutput = TestsOutputParser.getTestsOutput(output, isAdaptive);
    String stderr = output.getStderr();
    if (!stderr.isEmpty() && output.getStdout().isEmpty()) {
      LOG.info("#educational " + stderr);
      return new TestsOutputParser.TestsOutput(false, stderr);
    }
    return testsOutput;
  }

  public static void hideTestResultsToolWindow(@NotNull Project project) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TestResultsToolWindowFactoryKt.ID);
    if (toolWindow != null) {
      toolWindow.hide(() -> {});
    }
  }
}