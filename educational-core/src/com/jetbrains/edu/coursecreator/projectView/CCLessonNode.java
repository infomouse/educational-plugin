package com.jetbrains.edu.coursecreator.projectView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.jetbrains.edu.coursecreator.CourseInfoSynchronizer;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.projectView.LessonNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CCLessonNode extends LessonNode {
  public CCLessonNode(@NotNull Project project,
                      PsiDirectory value,
                      ViewSettings viewSettings,
                      @NotNull Lesson lesson) {
    super(project, value, viewSettings, lesson);
  }

  @Nullable
  @Override
  public AbstractTreeNode modifyChildNode(AbstractTreeNode childNode) {
    AbstractTreeNode node = super.modifyChildNode(childNode);
    if (node != null) {
      return node;
    }
    Object value = childNode.getValue();
    if (value instanceof PsiFile) {
      PsiFile psiFile = (PsiFile)value;
      if (CourseInfoSynchronizer.LESSON_CONFIG.equals(psiFile.getName())) {
        return new CCStudentInvisibleFileNode(myProject, psiFile, myViewSettings);
      }
    }
    return null;
  }

  @Override
  public PsiDirectoryNode createChildDirectoryNode(StudyItem item, PsiDirectory directory) {
    return new CCTaskNode(myProject, directory, myViewSettings, ((Task)item));
  }
}
