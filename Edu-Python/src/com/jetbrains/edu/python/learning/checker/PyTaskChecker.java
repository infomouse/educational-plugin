package com.jetbrains.edu.python.learning.checker;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.StudyState;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.CheckAction;
import com.jetbrains.edu.learning.checker.EduTaskChecker;
import com.jetbrains.edu.learning.checker.StudyCheckResult;
import com.jetbrains.edu.learning.checker.StudyCheckUtils;
import com.jetbrains.edu.learning.checker.StudyTestsOutputParser;
import com.jetbrains.edu.learning.core.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.StudyEditor;
import one.util.streamex.EntryStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class PyTaskChecker extends EduTaskChecker {
  private static final Logger LOG = Logger.getInstance(PyTaskChecker.class);

  public PyTaskChecker(EduTask task, Project project) {
    super(task, project);
  }

  @Override
  public StudyCheckResult check() {
    VirtualFile taskDir = myTask.getTaskDir(myProject);
    if (taskDir == null) {
      LOG.info("taskDir is null for task " + myTask.getName());
      return new StudyCheckResult(StudyStatus.Unchecked, "Task is broken");
    }

    if (!myTask.isValid(myProject)) {
      return new StudyCheckResult(StudyStatus.Unchecked,
              StudyEditor.BROKEN_SOLUTION_ERROR_TEXT_START + StudyEditor.ACTION_TEXT + StudyEditor.BROKEN_SOLUTION_ERROR_TEXT_END);
    }
    CountDownLatch latch = new CountDownLatch(1);
    ApplicationManager.getApplication()
      .invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
        StudyCheckUtils.flushWindows(myTask, taskDir);
        latch.countDown();
      }));
    final PyTestRunner testRunner = new PyTestRunner(myTask, taskDir);
    try {
      final VirtualFile fileToCheck = getTaskVirtualFile(myTask, taskDir);
      if (fileToCheck != null) {
        //otherwise answer placeholders might have been not flushed yet
        latch.await();
        Process testProcess = testRunner.createCheckProcess(myProject, fileToCheck.getPath());
        StudyTestsOutputParser.TestsOutput output =
          StudyCheckUtils
            .getTestOutput(testProcess, testRunner.getCommandLine().getCommandLineString(), myTask.getLesson().getCourse().isAdaptive());
        return new StudyCheckResult(output.isSuccess() ? StudyStatus.Solved : StudyStatus.Failed, output.getMessage());
      }
    }
    catch (ExecutionException | InterruptedException e) {
      LOG.error(e);
    }
    return new StudyCheckResult(StudyStatus.Unchecked, CheckAction.FAILED_CHECK_LAUNCH);
  }

  @Override
  public void clearState() {
    ApplicationManager.getApplication().invokeLater(() -> {
      StudyCheckUtils.drawAllPlaceholders(myProject, myTask);
      VirtualFile taskDir = myTask.getTaskDir(myProject);
      if (taskDir != null) {
        EduUtils.deleteWindowDescriptions(myTask, taskDir);
      }
    });
  }

  @Override
  public void onTaskFailed(@NotNull String message) {
    super.onTaskFailed(message);
    ApplicationManager.getApplication().invokeLater(() -> {
      VirtualFile taskDir = myTask.getTaskDir(myProject);
      if (taskDir == null) return;
      for (Map.Entry<String, TaskFile> entry : myTask.getTaskFiles().entrySet()) {
        final String name = entry.getKey();
        final TaskFile taskFile = entry.getValue();
        if (taskFile.getActivePlaceholders().size() < 2) {
          continue;
        }
        final Course course = myTask.getLesson().getCourse();
        if (course != null && course.isStudy()) {
          CommandProcessor.getInstance().runUndoTransparentAction(
            () -> ApplicationManager.getApplication().runWriteAction(
              () -> PySmartChecker.runSmartTestProcess(taskDir, new PyTestRunner(myTask, taskDir), name, taskFile, myProject)));
        }
      }
      StudyCheckUtils.navigateToFailedPlaceholder(new StudyState(StudyUtils.getSelectedStudyEditor(myProject)), myTask, taskDir, myProject);
    });
  }

  @Nullable
  private static VirtualFile getTaskVirtualFile(@NotNull final Task task,
                                                @NotNull final VirtualFile taskDir) {

    Map<TaskFile, VirtualFile> fileMap =
      EntryStream.of(task.getTaskFiles()).invert().mapValues(taskDir::findFileByRelativePath).nonNullValues().toMap();
    Map.Entry<TaskFile, VirtualFile> entry = EntryStream.of(fileMap).findAny(e -> !e.getKey().getActivePlaceholders().isEmpty())
      .orElse(fileMap.entrySet().stream().findFirst().orElse(null));
    return entry == null ? null : entry.getValue();
  }
}