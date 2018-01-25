package com.jetbrains.edu.learning.courseGeneration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.settings.CCSettings;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.intellij.EduIntellijUtils;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import kotlin.collections.MapsKt;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneratorUtils {
  private static final Logger LOG = Logger.getInstance(GeneratorUtils.class.getName());

  private GeneratorUtils() {}

  public static void createCourse(@NotNull final Course course, @NotNull final VirtualFile baseDir) {
    try {
      final List<Lesson> lessons = course.getLessons(true);
      for (int i = 1; i <= lessons.size(); i++) {
        Lesson lesson = lessons.get(i - 1);
        lesson.setIndex(i);
        createLesson(lesson, baseDir);
      }
      course.removeAdditionalLesson();
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  public static void createLesson(@NotNull final Lesson lesson, @NotNull final VirtualFile courseDir) throws IOException {
    if (EduNames.ADDITIONAL_MATERIALS.equals(lesson.getName())) {
      createAdditionalFiles(lesson, courseDir);
    }
    else {
      String lessonDirName = EduNames.LESSON + Integer.toString(lesson.getIndex());
      VirtualFile lessonDir = VfsUtil.createDirectoryIfMissing(courseDir, lessonDirName);
      final List<Task> taskList = lesson.getTaskList();
      for (int i = 1; i <= taskList.size(); i++) {
        Task task = taskList.get(i - 1);
        task.setIndex(i);
        createTask(task, lessonDir);
      }
    }
  }

  public static void createTask(@NotNull final Task task, @NotNull final VirtualFile lessonDir) throws IOException {
    String name = EduNames.TASK + Integer.toString(task.getIndex());
    VirtualFile taskDir = VfsUtil.createDirectoryIfMissing(lessonDir, name);
    createTaskContent(task, taskDir);
  }

  public static void createTaskContent(@NotNull Task task, @NotNull VirtualFile taskDir) throws IOException {
    int i = 0;
    for (Map.Entry<String, TaskFile> taskFile : task.getTaskFiles().entrySet()) {
      TaskFile taskFileContent = taskFile.getValue();
      taskFileContent.setIndex(i);
      i++;
      createTaskFile(taskDir, taskFile.getValue());
    }
    createTestFiles(taskDir, task);
    if (CCUtils.COURSE_MODE.equals(task.getLesson().getCourse().getCourseMode())) {
      createDescriptionFiles(taskDir, task);
    }
  }

  public static void createTaskFile(@NotNull final VirtualFile taskDir, @NotNull final TaskFile taskFile) throws IOException {
    createChildFile(taskDir, taskFile.getPathInTask(), taskFile.text);
  }

  public static void createTestFiles(@NotNull VirtualFile taskDir, @NotNull Task task) throws IOException {
    final Map<String, String> tests = TaskExt.getTestTextMap(task);
    createFiles(taskDir, tests);
  }

  public static void createDescriptionFiles(@NotNull VirtualFile taskDir, @NotNull Task task) throws IOException {
    final Map<String, String> taskTexts = task.getTaskTexts();
    Map<String, String> renamedTaskTexts = MapsKt.mapKeys(taskTexts, entry ->
            entry.getKey() + "." + FileUtilRt.getExtension(EduUtils.getTaskDescriptionFileName(CCSettings.getInstance().useHtmlAsDefaultTaskFormat())));
    createFiles(taskDir, renamedTaskTexts);
  }

  private static void createFiles(@NotNull VirtualFile taskDir, @NotNull Map<String, String> texts) throws IOException {
    for (Map.Entry<String, String> entry : texts.entrySet()) {
      final String name = entry.getKey();
      VirtualFile virtualTaskFile = taskDir.findChild(name);
      if (virtualTaskFile == null) {
        createChildFile(taskDir, name, entry.getValue());
      }
    }
  }

  private static void createAdditionalFiles(@NotNull Lesson lesson, @NotNull VirtualFile courseDir) throws IOException {
    final List<Task> taskList = lesson.getTaskList();
    if (taskList.size() != 1) return;
    final Task task = taskList.get(0);

    Map<String, String> filesToCreate = new HashMap<>(task.getTestsText());
    MapsKt.mapValuesTo(task.getTaskFiles(), filesToCreate, entry -> entry.getValue().text);
    filesToCreate.putAll(task.getAdditionalFiles());

    for (Map.Entry<String, String> entry : filesToCreate.entrySet()) {
      createChildFile(courseDir, entry.getKey(), entry.getValue());
    }
  }

  public static void createChildFile(@NotNull VirtualFile parentDir, @NotNull String path, @NotNull String text) throws IOException {
    String newDirectories = null;
    String fileName = path;
    VirtualFile dir = parentDir;
    if (path.contains("/")) {
      int pos = path.lastIndexOf("/");
      fileName = path.substring(pos + 1);
      newDirectories = path.substring(0, pos);
    }
    if (newDirectories != null) {
      dir = VfsUtil.createDirectoryIfMissing(parentDir, newDirectories);
    }
    if (dir != null) {
      VirtualFile virtualTaskFile = dir.findChild(fileName);
      if (virtualTaskFile == null) {
        virtualTaskFile = dir.createChildData(parentDir, fileName);
      }
      if (EduUtils.isImage(path)) {
        virtualTaskFile.setBinaryContent(Base64.decodeBase64(text));
      }
      else {
        VfsUtil.saveText(virtualTaskFile, text);
      }
    }
  }

  @NotNull
  public static Course initializeCourse(@NotNull Project project, @NotNull Course course) {
    if (course instanceof RemoteCourse) {
      course = getCourseFromStepik(project, (RemoteCourse)course);
    }
    course.initCourse(false);

    if (course.isAdaptive() && !EduUtils.isCourseValid(course)) {
      Messages.showWarningDialog("There is no recommended tasks for this adaptive course",
          "Error in Course Creation");
      return course;
    }
    if (updateTaskFilesNeeded(course)) {
      updateJavaCodeTaskFileNames(project, course);
    }
    StudyTaskManager.getInstance(project).setCourse(course);
    return course;
  }

  private static boolean updateTaskFilesNeeded(@NotNull final Course course) {
    return course instanceof RemoteCourse && course.isStudy() && EduNames.JAVA.equals(course.getLanguageID());
  }

  private static void updateJavaCodeTaskFileNames(@NotNull Project project, @NotNull Course course) {
    for (Lesson lesson : course.getLessons()) {
      for (Task task : lesson.getTaskList()) {
        if (task instanceof CodeTask) {
          for (TaskFile taskFile : task.getTaskFiles().values()) {
            EduIntellijUtils.nameTaskFileAfterContainingClass(task, taskFile, project);
          }
        }
      }
    }
  }

  private static RemoteCourse getCourseFromStepik(@NotNull Project project, RemoteCourse selectedCourse) {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      ProgressManager.getInstance().getProgressIndicator().setIndeterminate(false);
      final RemoteCourse course = StepikConnector.getCourse(project, selectedCourse);
      if (EduUtils.isCourseValid(course)) {
        course.initCourse(false);
      }
      return course;
    }, "Creating Course", true, project);
  }
}
