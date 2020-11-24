package org.jboss.tools.intellij.mta.explorer;

import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jboss.tools.intellij.mta.services.ModelService;
import org.jetbrains.annotations.NotNull;

public class MtaExplorerFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(Project project, @NotNull ToolWindow toolWindow) {
        ModelService modelService = new ModelService(project);
        modelService.loadModel();
        MtaToolWindow panel = new MtaToolWindow(modelService, project, toolWindow);
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(panel, null, false);
        contentManager.addContent(content);
        Disposer.register(content, modelService);
    }
}
