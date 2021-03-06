package com.jetbrains.edu.learning.twitter;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides twitting to plugin<br>
 * Example can be found in
 * <a href="https://github.com/JetBrains/educational-plugins/blob/master/Edu-Utils/Edu-Kotlin/src/com/jetbrains/edu/kotlin/EduKotlinTwitterConfigurator.java">Edu Kotlin</a> plugin
 * @see TwitterAction
 * */
public interface TwitterPluginConfigurator {
  ExtensionPointName<TwitterPluginConfigurator> EP_NAME = ExtensionPointName.create("Educational.twitterPluginConfigurator");

  /**
   * To implement tweeting you should register you app in twitter. For registered application twitter provide
   * consumer key and consumer secret, that are used for authorize by OAuth.
   * @return consumer key for current educational plugin
   */
  @NotNull
  String getConsumerKey(@NotNull final Project project);

  /**
   * To implement tweeting you should register you app in twitter. For registered application twitter provide
   * consumer key and consumer secret, that are used for authorize by OAuth.
   * @return consumer secret for current educational plugin
   */
  @NotNull String getConsumerSecret(@NotNull final Project project);

  /**
   * The plugin implemented tweeting should define policy when user will be asked to tweet.
   *@param statusBeforeCheck @return 
   */
  boolean askToTweet(@NotNull final Project project, Task solvedTask, CheckStatus statusBeforeCheck);

  /**
   * Stores access token and token secret, obtained by authorizing EduTools.
   */
  void storeTwitterTokens(@NotNull final Project project, @NotNull final String accessToken, @NotNull final String tokenSecret);

  /**
   * @return stored access token
   */
  @NotNull String getTwitterAccessToken(@NotNull Project project);

  /**
   * @return stored token secret
   */
  @NotNull String getTwitterTokenSecret(@NotNull Project project);

  /**
   * @return panel that will be shown to user in ask to tweet dialog. 
   */
  @Nullable
  TwitterUtils.TwitterDialogPanel getTweetDialogPanel(@NotNull Task solvedTask);
  
  void setAskToTweet(@NotNull Project project, boolean askToTweet);

  boolean accept(@NotNull final Project project);
}
