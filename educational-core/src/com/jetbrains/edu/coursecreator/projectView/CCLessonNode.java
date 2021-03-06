package com.jetbrains.edu.coursecreator.projectView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.projectView.LessonNode;
import org.jetbrains.annotations.NotNull;

public class CCLessonNode extends LessonNode {
  public CCLessonNode(@NotNull Project project,
                      PsiDirectory value,
                      ViewSettings viewSettings,
                      @NotNull Lesson lesson) {
    super(project, value, viewSettings, lesson);
  }

  @Override
  public PsiDirectoryNode createChildDirectoryNode(StudyItem item, PsiDirectory directory) {
    return new CCTaskNode(myProject, directory, myViewSettings, ((Task)item));
  }
}
