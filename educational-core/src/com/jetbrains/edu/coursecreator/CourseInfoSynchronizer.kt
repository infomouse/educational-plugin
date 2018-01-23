package com.jetbrains.edu.coursecreator

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.yaml.snakeyaml.Yaml


object CourseInfoSynchronizer {

  fun dumpCourseInfo(project: Project) {
    val course = StudyTaskManager.getInstance(project).course!!
    val lessonNames = course.lessons.sortedBy { it.index }.map { it.name }
    val map = mapOf<String, Any>(Pair("name", course.name), Pair("language", course.humanLanguage),
                                 Pair("programming_language", course.languageById.displayName),
                                 Pair("description", course.description),
                                 Pair("lessons", lessonNames))
    dumpData(map, project.baseDir, "course-info.yaml")
  }

  fun dumpLesson(lessonDir: VirtualFile, lesson: Lesson) {
    val tasks = lesson.getTaskList().sortedBy { it.index }.map { it.name }
    dumpData(mapOf(Pair("tasks", tasks)), lessonDir, "lesson-info.yaml")
  }

  fun dumpTask(taskDir: VirtualFile, task: Task) {
    dumpData(mapOf(Pair("type", task.taskType)), taskDir, "task-info.yaml")
  }


  fun synchronizeCourse(project: Project, courseInfo: String) {
    val course = StudyTaskManager.getInstance(project).course!!
    val courseProperties = Yaml().loadAs(courseInfo, Map::class.java)
    course.name = courseProperties["name"] as String
    ProjectView.getInstance(project).refresh()
  }

  private fun dumpData(data: Map<String, Any>, dir: VirtualFile, fileName: String) {
    val dump = Yaml().dumpAsMap(data)
    ApplicationManager.getApplication().runWriteAction {
      val file = dir.findOrCreateChildData(CourseInfoSynchronizer.javaClass, fileName)
      VfsUtil.saveText(file, dump)
    }
  }
}