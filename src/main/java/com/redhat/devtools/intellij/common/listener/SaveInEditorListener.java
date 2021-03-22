/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.listener;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentSynchronizationVetoer;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.common.utils.YAMLHelper;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static com.redhat.devtools.intellij.common.CommonConstants.LAST_MODIFICATION_STAMP;
import static com.redhat.devtools.intellij.common.CommonConstants.PROJECT;
import static com.redhat.devtools.intellij.common.CommonConstants.TARGET_NODE;

public abstract class SaveInEditorListener extends FileDocumentSynchronizationVetoer {
    @Override
    public boolean maySaveDocument(@NotNull Document document, boolean isSaveExplicit) {
        VirtualFile vf = FileDocumentManager.getInstance().getFile(document);
        Project project = vf.getUserData(PROJECT);
        Long lastModificationStamp = vf.getUserData(LAST_MODIFICATION_STAMP);
        Long currentModificationStamp = document.getModificationStamp();
        if (project == null ||
                !isFileToPush(project, vf) ||
                currentModificationStamp.equals(lastModificationStamp)
        ) {
            return true;
        }

        vf.putUserData(LAST_MODIFICATION_STAMP, currentModificationStamp);
        if (save(document, project)) {
            notify(document);
            refresh(project, vf.getUserData(TARGET_NODE));
        }
        return false;
    }

    protected abstract void notify(Document document);

    protected abstract void refresh(Project project, Object node);

    protected abstract boolean save(Document document, Project project);

    protected boolean isFileToPush(Project project, VirtualFile vf) {
        FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        // if file is not the one selected, skip it
        return !(selectedEditor == null || !selectedEditor.getFile().equals(vf));
    }
}
