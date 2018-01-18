package com.jetbrains.edu.coursecreator

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.StudyTaskManager
import org.yaml.snakeyaml.Yaml


object CourseInfoSynchronizer {

  fun dumpCourseInfo(project: Project) {
    val course = StudyTaskManager.getInstance(project).course!!
    val map = mapOf(Pair("name", course.name), Pair("language", course.humanLanguage),
                    Pair("programming_language", course.languageById.displayName),
                    Pair("description", course.description))
    val dump = Yaml().dumpAsMap(map)
    ApplicationManager.getApplication().runWriteAction {
      val file = project.baseDir.findOrCreateChildData(CourseInfoSynchronizer.javaClass, "course-info.yaml")
      VfsUtil.saveText(file, dump)
    }
  }
}

object CourseSynchronizer {
  fun synchronizeCourse(project: Project, courseInfo: String) {
    val course = StudyTaskManager.getInstance(project).course!!
    val courseProperties = Yaml().loadAs(courseInfo, Map::class.java)
    course.name = courseProperties["name"] as String
    ProjectView.getInstance(project).refresh()
  }

}