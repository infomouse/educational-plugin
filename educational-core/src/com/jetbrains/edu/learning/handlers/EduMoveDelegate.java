package com.jetbrains.edu.learning.handlers;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandlerDelegate;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.Nullable;

public class EduMoveDelegate extends MoveHandlerDelegate{
  @Override
  public boolean canMove(DataContext dataContext) {
    return EduUtils.canRenameOrMove(dataContext);
  }

  @Override
  public boolean canMove(PsiElement[] elements, @Nullable PsiElement targetContainer) {
    if (elements.length == 1) {
      Project project = elements[0].getProject();
      Course course = StudyTaskManager.getInstance(project).getCourse();
      if (course == null || !course.isStudy()) {
        return false;
      }
      return !EduUtils.isRenameableOrMoveable(project, course, elements[0]);
    }
    return false;
  }

  @Override
  public boolean isValidTarget(PsiElement psiElement, PsiElement[] sources) {
    return true;
  }

  @Override
  public void doMove(final Project project,
                     PsiElement[] elements,
                     @Nullable PsiElement targetContainer,
                     @Nullable MoveCallback callback) {
    Messages.showInfoMessage("This move operation can break the course", "Invalid Move Operation");
  }

  @Override
  public boolean tryToMove(PsiElement element,
                           Project project,
                           DataContext dataContext,
                           @Nullable PsiReference reference,
                           Editor editor) {
    return EduUtils.isStudyProject(project);
  }
}
