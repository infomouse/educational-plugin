package com.jetbrains.edu.learning.courseGeneration;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.jetbrains.edu.learning.stepic.StepicConnector;
import com.jetbrains.edu.learning.stepic.StepicNames;
import com.jetbrains.edu.learning.stepic.StepicUser;
import com.jetbrains.edu.learning.stepic.StepikSolutionsLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.edu.learning.EduUtils.execCancelable;

public class ProjectGenerator {
  private static final Logger LOG = Logger.getInstance(ProjectGenerator.class.getName());
  private final List<SettingsListener> myListeners = ContainerUtil.newArrayList();
  private List<Course> myCourses = new ArrayList<>();
  private List<Integer> myEnrolledCoursesIds = new ArrayList<>();
  protected Course mySelectedCourse;

  public void setCourses(List<Course> courses) {
    myCourses = courses;
  }

  public boolean isLoggedIn() {
    final StepicUser user = EduSettings.getInstance().getUser();
    return user != null;
  }

  public void setEnrolledCoursesIds(@NotNull final List<Integer> coursesIds) {
    myEnrolledCoursesIds = coursesIds;
  }

  @NotNull
  public List<Integer> getEnrolledCoursesIds() {
    return myEnrolledCoursesIds;
  }

  public void setSelectedCourse(@NotNull final Course course) {
    mySelectedCourse = course;
  }

  public Course getSelectedCourse() {
    return mySelectedCourse;
  }

  public void generateProject(@NotNull final Project project, @NotNull final VirtualFile baseDir) {
    final Course course = getCourse(project);
    if (course == null) {
      LOG.warn("Course is null");
      Messages.showWarningDialog("Some problems occurred while creating the course", "Error in Course Creation");
      return;
    }
    else if (course.isAdaptive() && !EduUtils.isCourseValid(course)) {
      Messages.showWarningDialog("There is no recommended tasks for this adaptive course", "Error in Course Creation");
      return;
    }
    updateCourseFormat(course);
    StudyTaskManager.getInstance(project).setCourse(course);
    ApplicationManager.getApplication().runWriteAction(() -> {
      GeneratorUtils.createCourse(course, baseDir);
      EduUtils.registerStudyToolWindow(course, project);
      EduUtils.openFirstTask(course, project);
      EduUsagesCollector.projectTypeCreated(course.isAdaptive() ? EduNames.ADAPTIVE : EduNames.STUDY);

      if (course instanceof RemoteCourse && EduSettings.getInstance().getUser() != null) {
        StepikSolutionsLoader stepikSolutionsLoader = StepikSolutionsLoader.getInstance(project);
        stepikSolutionsLoader.loadSolutions(ProgressIndicatorProvider.getGlobalProgressIndicator(), course);
        EduUsagesCollector.progressOnGenerateCourse();
        PropertiesComponent.getInstance(project).setValue(StepicNames.ARE_SOLUTIONS_UPDATED_PROPERTY, true, false);
      }
    });
  }

  protected static void updateCourseFormat(@NotNull final Course course) {
    final List<Lesson> lessons = course.getLessons(true);
    final Lesson additionalLesson = lessons.stream().
        filter(lesson -> EduNames.PYCHARM_ADDITIONAL.equals(lesson.getName())).findFirst().orElse(null);
    if (additionalLesson != null) {
      additionalLesson.setName(EduNames.ADDITIONAL_MATERIALS);
      final List<Task> taskList = additionalLesson.getTaskList();
      taskList.stream().filter(task -> EduNames.PYCHARM_ADDITIONAL.equals(task.getName())).findFirst().
          ifPresent(task -> task.setName(EduNames.ADDITIONAL_MATERIALS));
    }
  }

  @Nullable
  public Course getCourse(@NotNull final Project project) {
    if (mySelectedCourse instanceof RemoteCourse) {
      return getCourseFromStepic(project, (RemoteCourse)mySelectedCourse);
    }
    mySelectedCourse.initCourse(false);
    return mySelectedCourse;
  }

  private static RemoteCourse getCourseFromStepic(@NotNull Project project, RemoteCourse selectedCourse) {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
      return execCancelable(() -> {
        final RemoteCourse course = StepicConnector.getCourse(project, selectedCourse);
        if (EduUtils.isCourseValid(course)) {
          course.initCourse(false);
        }
        return course;
      });
    }, "Creating Course", true, project);
  }

  // Supposed to be called under progress
  public List<Course> getCourses(boolean force) {
    if (force) {
      myCourses = execCancelable(() -> StepicConnector.getCourses(EduSettings.getInstance().getUser()));
    }
    List<Course> bundledCourses = getBundledCourses();
    if (bundledCourses != null) {
      for (Course bundledCourse : bundledCourses) {
        if (bundledCourse == null || myCourses.stream().anyMatch(course -> course.getName().equals(bundledCourse.getName()))) {
          continue;
        }
        myCourses.add(bundledCourse);
      }
    }
    sortCourses(myCourses);
    return myCourses;
  }

  public void sortCourses(List<Course> result) {
    // sort courses so as to have non-adaptive courses in the beginning of the list
    Collections.sort(result, (c1, c2) -> {
      if (mySelectedCourse != null) {
        if (mySelectedCourse.equals(c1)) {
          return -1;
        }
        if (mySelectedCourse.equals(c2)) {
          return 1;
        }
      }
      if ((c1.isAdaptive() && c2.isAdaptive()) || (!c1.isAdaptive() && !c2.isAdaptive())) {
        return 0;
      }
      return c1.isAdaptive() ? 1 : -1;
    });
  }

  @NotNull
  public List<Course> getCoursesUnderProgress(boolean force, @NotNull final String progressTitle, @Nullable final Project project) {
    try {
      return ProgressManager.getInstance()
        .runProcessWithProgressSynchronously(() -> {
          ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
          return getCourses(force);
        }, progressTitle, true, project);
    }
    catch (RuntimeException e) {
      return Collections.singletonList(Course.INVALID_COURSE);
    }
  }

  public void addSettingsStateListener(@NotNull SettingsListener listener) {
    myListeners.add(listener);
  }

  public interface SettingsListener {
    void stateChanged(ValidationResult result);
  }

  public void fireStateChanged(ValidationResult result) {
    for (SettingsListener listener : myListeners) {
      listener.stateChanged(result);
    }
  }

  @Nullable
  public List<Course> getBundledCourses() {
    final ArrayList<Course> courses = new ArrayList<>();
    final List<LanguageExtensionPoint<EduConfigurator<?>>> extensions = EduConfiguratorManager.allExtensions();
    for (LanguageExtensionPoint<EduConfigurator<?>> extension : extensions) {
      final EduConfigurator configurator = extension.getInstance();
      final List<String> paths = configurator.getBundledCoursePaths();
      for (String path : paths) {
        courses.add(getCourse(path));
      }
    }
    return courses;
  }

  @Nullable
  public Course addLocalCourse(String zipFilePath) {
    final Course courseInfo = getCourse(zipFilePath);
    if (courseInfo != null) {
      myCourses.add(0, courseInfo);
    }
    return courseInfo;
  }

  @Nullable
  public static Course getCourse(String zipFilePath) {
    try {
      final JBZipFile zipFile = new JBZipFile(zipFilePath);
      final JBZipEntry entry = zipFile.getEntry(EduNames.COURSE_META_FILE);
      if (entry == null) {
        return null;
      }
      byte[] bytes = entry.getData();
      final String jsonText = new String(bytes, CharsetToolkit.UTF8_CHARSET);
      Gson gson = new GsonBuilder()
        .registerTypeAdapter(Task.class, new SerializationUtils.Json.TaskAdapter())
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
      return gson.fromJson(jsonText, Course.class);
    }
    catch (IOException e) {
      LOG.error("Failed to unzip course archive");
      LOG.error(e);
    }
    return null;
  }
}