package com.jetbrains.edu.learning.courseFormat.tasks;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.EduPluginConfigurator;
import com.jetbrains.edu.learning.EduPluginConfiguratorManager;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;

/**
 * Original Edu plugin tasks with local tests and answer placeholders
 */
public class EduTask extends Task {

  public EduTask() {
  }

  public EduTask(@NotNull String name) {
    super(name);
  }

  @Override
  public String getTaskType() {
    return "edu";
  }

  @Override
  public TaskChecker getChecker(@NotNull Project project) {
    Course course = getLesson().getCourse();
    EduPluginConfigurator configurator = EduPluginConfiguratorManager.forLanguage(course.getLanguageById());
    return configurator != null ? configurator.getEduTaskChecker(this, project) : super.getChecker(project);
  }
}