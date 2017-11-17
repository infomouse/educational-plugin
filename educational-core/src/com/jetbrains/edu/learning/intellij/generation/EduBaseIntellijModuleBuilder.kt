package com.jetbrains.edu.learning.intellij.generation

import com.intellij.ide.util.projectWizard.JavaModuleBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleWithNameAlreadyExists
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.jetbrains.edu.learning.EduConfigurator
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.Course
import org.jdom.JDOMException
import java.io.IOException

abstract class EduBaseIntellijModuleBuilder : JavaModuleBuilder() {

  @Throws(InvalidDataException::class,
          IOException::class,
          ModuleWithNameAlreadyExists::class,
          JDOMException::class,
          ConfigurationException::class)
  override fun createAndCommitIfNeeded(project: Project, model: ModifiableModuleModel?, runFromProjectWizard: Boolean): Module {
    val module = super.createAndCommitIfNeeded(project, model, runFromProjectWizard)
    val course = course ?: return module
    val configurator = configurator(course) ?: return module
    configurator.courseBuilder.configureModule(module)
    return module
  }

  open protected val course: Course? get() = null

  protected fun configurator(course: Course): EduConfigurator<*>? {
    val language = course.languageById
    if (language == null) {
      LOG.error("Can't find language by ${course.languageID}")
      return null
    }
    val configurator = EduConfiguratorManager.forLanguage(language)
    if (configurator == null) {
      LOG.error("EduConfigurator for language ${language.displayName} not found")
      return null
    }
    return configurator
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(EduBaseIntellijModuleBuilder::class.java)
  }
}
