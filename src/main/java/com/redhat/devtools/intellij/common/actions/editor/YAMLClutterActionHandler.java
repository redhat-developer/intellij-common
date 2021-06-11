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
package com.redhat.devtools.intellij.common.actions.editor;

import com.google.common.base.Strings;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static com.redhat.devtools.intellij.common.CommonConstants.CLEANED;
import static com.redhat.devtools.intellij.common.CommonConstants.CONTENT;

public abstract class YAMLClutterActionHandler extends EditorWriteActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(RestoreYAMLClutterActionHandler.class);

    @Override
    public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        VirtualFile vf = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (!Strings.isNullOrEmpty(vf.getUserData(CONTENT))) {
            editor.getDocument().setText(getUpdatedContent(vf.getUserData(CONTENT), editor.getDocument().getText()));
            vf.putUserData(CLEANED, isCleaned());
        }
    }

    @Override
    public void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        VirtualFile vf = FileDocumentManager.getInstance().getFile(editor.getDocument());
        boolean isWritable = vf.isWritable();
        try {
            editor.getDocument().setReadOnly(false);
            vf.setWritable(true);
            super.doExecute(editor, caret, dataContext);
            vf.setWritable(isWritable);
            editor.getDocument().setReadOnly(!isWritable);
        } catch (IOException e) {
            logger.warn(e.getLocalizedMessage(), e);
        }
    }

    public abstract String getUpdatedContent(String originalContent, String currentContent);
    public abstract boolean isCleaned();
}
