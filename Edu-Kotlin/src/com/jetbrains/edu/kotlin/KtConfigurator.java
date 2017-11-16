package com.jetbrains.edu.kotlin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.intellij.EduConfiguratorBase;
import com.jetbrains.edu.learning.intellij.JdkProjectSettings;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class KtConfigurator extends EduConfiguratorBase {

  static final String LEGACY_TESTS_KT = "tests.kt";
  public static final String TESTS_KT = "Tests.kt";
  private static final String SUBTASK_TESTS_KT = "Subtask_Tests.kt";
  public static final String TASK_KT = "Task.kt";

  private final KtCourseBuilder myCourseBuilder = new KtCourseBuilder();

  @NotNull
  @Override
  public EduCourseBuilder<JdkProjectSettings> getCourseBuilder() {
    return myCourseBuilder;
  }

  @NotNull
  @Override
  public String getTestFileName() {
    return TESTS_KT;
  }

  @Override
  public boolean isTestFile(VirtualFile file) {
    String name = file.getName();
    return TESTS_KT.equals(name) || LEGACY_TESTS_KT.equals(name) || name.contains(FileUtil.getNameWithoutExtension(TESTS_KT)) && name.contains(EduNames.SUBTASK_MARKER);
  }

  @NotNull
  @Override
  public TaskChecker<EduTask> getEduTaskChecker(@NotNull EduTask eduTask, @NotNull Project project) {
    return new KtTaskChecker(eduTask, project);
  }

  @Override
  public List<String> getBundledCoursePaths() {
    File bundledCourseRoot = EduUtils.getBundledCourseRoot(KtKotlinKoansModuleBuilder.DEFAULT_COURSE_NAME, KtKotlinKoansModuleBuilder.class);
    return Collections.singletonList(FileUtil.join(bundledCourseRoot.getAbsolutePath(), KtKotlinKoansModuleBuilder.DEFAULT_COURSE_NAME));
  }

  @Override
  public boolean isEnabled() {
    return !EduUtils.isAndroidStudio();
  }
}
