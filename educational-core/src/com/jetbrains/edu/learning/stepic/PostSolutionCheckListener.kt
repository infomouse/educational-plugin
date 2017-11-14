package com.jetbrains.edu.learning.stepic

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.StudyCheckListener
import com.jetbrains.edu.learning.courseFormat.StudyStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class PostSolutionCheckListener : StudyCheckListener {
    override fun afterCheck(project: Project, task: Task) {
        val course = StudyTaskManager.getInstance(project).course
        val status = task.status
        if (EduSettings.getInstance().user != null && course != null && course.isStudy && status != StudyStatus.Unchecked) {
            EduStepicConnector.postSolution(task, status == StudyStatus.Solved, project)
        }
    }
}