package com.jetbrains.edu.coursecreator.actions;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.jetbrains.edu.coursecreator.settings.CCSettings;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class CCCreateTask extends CCCreateStudyItemActionBase<Task> {
  public static final String TITLE = "Create New " + EduNames.TASK_TITLED;

  public CCCreateTask() {
    super(EduNames.TASK_TITLED, TITLE, EducationalCoreIcons.Task);
  }

  @Nullable
  @Override
  protected VirtualFile getParentDir(@NotNull Project project, @NotNull Course course, @NotNull VirtualFile directory) {
    if (isAddedAsLast(directory, project, course)) {
      return directory;
    }
    return directory.getParent();
  }

  @Override
  protected void addItem(@NotNull Course course, @NotNull Task item) {
    item.getLesson().addTask(item);
  }

  @Override
  protected Function<VirtualFile, ? extends StudyItem> getStudyOrderable(@NotNull final StudyItem item) {
    return (Function<VirtualFile, StudyItem>)file -> {
      if (item instanceof Task) {
        return ((Task)item).getLesson().getTask(file.getName());
      }
      return null;
    };
  }

  @Override
  @Nullable
  protected VirtualFile createItemDir(@NotNull final Project project, @NotNull final Task item,
                                      @NotNull final VirtualFile parentDirectory, @NotNull final Course course) {
    EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());
    if (configurator != null) {
      return configurator.getCourseBuilder().createTaskContent(project, item, parentDirectory, course);
    }
    return null;
  }

  @Override
  protected int getSiblingsSize(@NotNull Course course, @Nullable StudyItem parentItem) {
    if (parentItem instanceof Lesson) {
      return ((Lesson)parentItem).getTaskList().size();
    }
    return 0;
  }

  @Nullable
  @Override
  protected StudyItem getParentItem(@NotNull Course course, @NotNull VirtualFile directory) {
    Task task = (Task)getThresholdItem(course, directory);
    if (task == null) {
      return course.getLesson(directory.getName());
    }
    return task.getLesson();
  }

  @Nullable
  @Override
  protected StudyItem getThresholdItem(@NotNull Course course, @NotNull VirtualFile sourceDirectory) {
    return EduUtils.getTask(sourceDirectory, course);
  }

  @Override
  protected boolean isAddedAsLast(@NotNull VirtualFile sourceDirectory,
                                  @NotNull Project project,
                                  @NotNull Course course) {
    return course.getLesson(sourceDirectory.getName()) != null;
  }

  @Override
  protected void sortSiblings(@NotNull Course course, @Nullable StudyItem parentItem) {
    if (parentItem instanceof Lesson) {
      Collections.sort(((Lesson)parentItem).getTaskList(), EduUtils.INDEX_COMPARATOR);
    }
  }

  @Override
  protected String getItemName() {
    return EduNames.TASK;
  }

  @Override
  public Task createAndInitItem(@NotNull Course course, @Nullable StudyItem parentItem, String name, int index) {
    final Task task = new EduTask(name);
    task.setIndex(index);
    if (parentItem == null) {
      return null;
    }
    task.setLesson(((Lesson)parentItem));
    String fileName = EduUtils.getTaskDescriptionFileName(CCSettings.getInstance().useHtmlAsDefaultTaskFormat());
    FileTemplate template = FileTemplateManager.getDefaultInstance().getInternalTemplate(fileName);
    if (template != null) {
      task.addTaskText(EduNames.TASK, template.getText());
    }
    return task;
  }
}