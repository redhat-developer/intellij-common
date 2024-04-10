/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.actions;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class StructureTreeAction extends TreeAction {
    public StructureTreeAction(Class... filters) {
        super(filters);
    }

    public StructureTreeAction(boolean acceptMultipleItems, Class... filters) {
        super(acceptMultipleItems, filters);
    }

    public static <T> T getElement(Object selected) {
        if (selected instanceof DefaultMutableTreeNode) {
            selected = ((DefaultMutableTreeNode)selected).getUserObject();
        }
        if (selected instanceof NodeDescriptor) {
            selected = ((NodeDescriptor)selected).getElement();
        }
        return (T) selected;
    }

    @Override
    protected Object adjust(Object selected) {
        return getElement(selected);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.EDT;
    }
}
