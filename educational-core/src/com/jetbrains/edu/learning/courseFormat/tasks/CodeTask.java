package com.jetbrains.edu.learning.courseFormat.tasks;

import org.jetbrains.annotations.NotNull;

public class CodeTask extends Task {
  @SuppressWarnings("unused") //used for deserialization
  public CodeTask() {}

  public CodeTask(@NotNull final String name) {
    super(name);
  }

  private boolean hasClass = true;
  public static final String JAVA_PREFIX = "public class Main {\n\n";
  public static final String JAVA_POSTFIX = "\n\n}\n";

  @Override
  public String getTaskType() {
    return "code";
  }

  public void setHasClass(boolean hasClass) {
    this.hasClass = hasClass;
  }

  public boolean getHasClass() {
    return hasClass;
  }
}
