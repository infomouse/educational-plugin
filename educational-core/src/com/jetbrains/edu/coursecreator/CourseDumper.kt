package com.jetbrains.edu.coursecreator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.StudyTaskManager
import org.yaml.snakeyaml.Yaml


object CourseDumper {

  fun dumpCourse(project: Project) {
    val course = StudyTaskManager.getInstance(project).course!!
    val map = mapOf(Pair("name", course.name), Pair("language", course.humanLanguage),
                    Pair("programming_language", course.languageById.displayName),
                    Pair("description", course.description))
    val dump = Yaml().dumpAsMap(map)
    ApplicationManager.getApplication().runWriteAction {
      val file = project.baseDir.findOrCreateChildData(CourseDumper.javaClass, "course-info.yaml")
      VfsUtil.saveText(file, dump)
    }
  }
}