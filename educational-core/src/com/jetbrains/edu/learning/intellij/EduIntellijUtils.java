package com.jetbrains.edu.learning.intellij;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

/**
 * Contains code shared between Java and Kotlin modules
 */
public class EduIntellijUtils {
  private static final Logger LOG = Logger.getInstance(EduIntellijUtils.class);

  private EduIntellijUtils() {
  }

  public static void nameTaskFileAfterContainingClass(@NotNull Task task,
                                                      @NotNull TaskFile taskFile,
                                                      @NotNull Project project) {
    Language language = task.getLesson().getCourse().getLanguageById();
    final LanguageFileType associatedFileType = language.getAssociatedFileType();
    if (associatedFileType == null) {
      LOG.warn("Cannot rename task file. Unable to find associated file type for language: " + language.getID());
      return;
    }
    task.getTaskFiles().remove(taskFile.name);
    taskFile.name = publicClassName(project, (CodeTask)task, taskFile,
        associatedFileType) + "." + associatedFileType.getDefaultExtension();
    task.taskFiles.put(taskFile.name, taskFile);
  }

  @NotNull
  private static String publicClassName(@NotNull Project project, CodeTask task, @NotNull TaskFile taskFile, @NotNull LanguageFileType fileType) {
    String fileName = "Main";
    PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(taskFile.name, fileType, taskFile.text);
    if (file instanceof PsiClassOwner) {
      PsiClassOwner fileFromText = (PsiClassOwner) file;
      PsiClass[] classes = fileFromText.getClasses();
      if (classes.length == 0) {
        task.setHasClass(false);
        final String text = taskFile.text;
        taskFile.text = CodeTask.JAVA_PREFIX + text + CodeTask.JAVA_POSTFIX;
      }
      for (PsiClass aClass : classes) {
        boolean isPublic = aClass.hasModifierProperty(PsiModifier.PUBLIC) || "Main".equals(aClass.getName());
        if (isPublic && aClass.getName() != null) {
          fileName = aClass.getName();
          break;
        }
      }
    }

    return fileName;
  }

}
