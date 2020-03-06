package com.drcuiyutao.idea.plugin.action;

import com.drcuiyutao.idea.plugin.util.LogUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class InsertAction extends AnAction {

    private static final String TAG = "InsertAction";

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        LogUtil.i(TAG, "actionPerformed event[" + event + "]");
        Project project = event.getProject();
        Messages.showMessageDialog(project, "Hello world!", "Greeting", Messages.getInformationIcon());
    }
}
