package com.jetbrains.edu.learning.intellij.generation

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.io.IOException

open class GradleCourseProjectGenerator(
  private val myCourseBuilder: GradleCourseBuilderBase,
  course: Course
) : CourseProjectGenerator<JdkProjectSettings>(course) {

  override fun createCourseStructure(project: Project, baseDir: VirtualFile, settings: JdkProjectSettings) {
    PropertiesComponent.getInstance(project).setValue(SHOW_UNLINKED_GRADLE_POPUP, false, true)
    runWriteAction {
      try {
        GeneratorUtils.initializeCourse(project, myCourse)
        if (CCUtils.isCourseCreator(project) && myCourse.getLessons(true).isEmpty()) {
          val lesson = CCCreateLesson().createAndInitItem(myCourse, null, EduNames.LESSON + 1, 1)
          myCourse.addLesson(lesson)
          val task = CCCreateTask().createAndInitItem(myCourse, lesson, EduNames.TASK + 1, 1)
          lesson.addTask(task)
          myCourseBuilder.initNewTask(task)
        }
        EduGradleModuleGenerator.createProjectGradleFiles(project.basePath!!, project.name, myCourseBuilder.buildGradleTemplateName)
        EduGradleModuleGenerator.createCourseContent(myCourse, baseDir)

        setJdk(project, settings)
      } catch (e: IOException) {
        LOG.error("Failed to generate course", e)
      }
    }
  }

  private fun setJdk(project: Project, settings: JdkProjectSettings) {
    val jdk = getJdk(settings)

    // Try to apply model, i.e. commit changes from sdk model into ProjectJdkTable
    try {
      settings.model.apply()
    } catch (e: ConfigurationException) {
      LOG.error(e)
    }

    runWriteAction { ProjectRootManager.getInstance(project).projectSdk = jdk }
  }

  private fun getJdk(settings: JdkProjectSettings): Sdk? {
    val selectedItem = settings.jdkItem ?: return null
    if (selectedItem is JdkComboBox.SuggestedJdkItem) {
      val type = selectedItem.sdkType
      val path = selectedItem.path
      val jdkRef = Ref<Sdk>()
      settings.model.addSdk(type, path, Consumer<Sdk> { jdkRef.set(it) })
      return jdkRef.get()
    }
    return selectedItem.jdk
  }

  companion object {

    private val LOG = Logger.getInstance(GradleCourseProjectGenerator::class.java)
    // Unfortunately, org.jetbrains.plugins.gradle.service.project.GradleStartupActivity#SHOW_UNLINKED_GRADLE_POPUP is private
    // so create own const
    private val SHOW_UNLINKED_GRADLE_POPUP = "show.inlinked.gradle.project.popup"
  }
}
