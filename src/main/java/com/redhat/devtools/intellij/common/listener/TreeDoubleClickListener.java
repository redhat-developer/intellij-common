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

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.ui.DoubleClickListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;


public abstract class TreeDoubleClickListener extends DoubleClickListener {
    private final JTree tree;

    public TreeDoubleClickListener(final JTree tree) {
        this.tree = tree;
        installOn(tree);
    }

    @Override
    protected boolean onDoubleClick(MouseEvent event) {
        final TreePath clickPath = tree.getPathForLocation(event.getX(), event.getY());
        if (clickPath == null) {
            return false;
        }

        final DataContext dataContext = DataManager.getInstance().getDataContext(tree);
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) {
            return false;
        }

        TreePath selectionPath = tree.getSelectionPath();
        if (!clickPath.equals(selectionPath)) {
            return false;
        }

        if (event.getClickCount() == 2) {
            processDoubleClick(selectionPath);
            return true;
        }
        return false;
    }

    protected abstract void processDoubleClick(@NotNull TreePath treePath);
}
