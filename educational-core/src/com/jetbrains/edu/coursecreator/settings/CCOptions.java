package com.jetbrains.edu.coursecreator.settings;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.wm.IdeFocusManager;
import com.jetbrains.edu.learning.settings.OptionsProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction.COURSE_CREATOR_ENABLED;

public class CCOptions implements OptionsProvider {
  private JRadioButton myHtmlRadioButton;
  private JRadioButton myMarkdownRadioButton;
  private JPanel myPanel;

  @Nullable
  @Override
  public JComponent createComponent() {
    if (!PropertiesComponent.getInstance().getBoolean(COURSE_CREATOR_ENABLED)) return null;
    if (CCSettings.getInstance().useHtmlAsDefaultTaskFormat()) {
      myHtmlRadioButton.setSelected(true);
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
        () -> IdeFocusManager.getGlobalInstance().requestFocus(myHtmlRadioButton, true));
    }
    else {
      myMarkdownRadioButton.setSelected(true);
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
        () -> IdeFocusManager.getGlobalInstance().requestFocus(myMarkdownRadioButton, true));
    }
    return myPanel;
  }

  @Override
  public boolean isModified() {
    final boolean htmlAsDefaultTaskFormat = CCSettings.getInstance().useHtmlAsDefaultTaskFormat();
    return myHtmlRadioButton.isSelected() != htmlAsDefaultTaskFormat;
  }

  @Override
  public void apply() {
    if (isModified()) {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(myHtmlRadioButton.isSelected());
    }
  }

  @Override
  public void reset() {
    createComponent();    
  }

  @Override
  public void disposeUIResources() {

  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Course Creator options";
  }
}
