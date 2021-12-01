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
package com.redhat.devtools.intellij.common.tree;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ui.tree.StructureTreeModel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;
import java.util.function.Supplier;

public class MutableModelSynchronizer<T> implements MutableModel.Listener<T> {
    protected final StructureTreeModel treeModel;
    private final AbstractTreeStructure structure;
    private final MutableModel<T> mutableModel;

    public MutableModelSynchronizer(StructureTreeModel treeModel,
                                    AbstractTreeStructure structure,
                                    MutableModel<T> mutableModel) {
        this.treeModel = treeModel;
        this.structure = structure;
        this.mutableModel = mutableModel;
        this.mutableModel.addListener(this);
    }

    private void invalidatePath(Supplier<TreePath> pathSupplier) {
        treeModel.getInvoker().invokeLater(() -> {
            TreePath path = pathSupplier.get();
            if (path == null) {
                return;
            }
            if (path.getLastPathComponent() == treeModel.getRoot()) {
                invalidateRoot();
            }
            treeModel.invalidate(path, true);
        });
    }

    private void invalidateRoot() {
        treeModel.invalidate();
    }

    private T getParentElement(T element) {
        return (T) structure.getParentElement(element);
    }

    protected TreePath getTreePath(T element) {
        TreePath path;
        if (isRootNode(element)) {
            path = new TreePath(treeModel.getRoot());
        } else {
            path = findTreePath(element, (DefaultMutableTreeNode)treeModel.getRoot());
        }
        return path!=null?path:new TreePath(treeModel.getRoot());
    }

    protected boolean isRootNode(T element) {
        NodeDescriptor descriptor = (NodeDescriptor) ((DefaultMutableTreeNode)treeModel.getRoot()).getUserObject();
        return descriptor != null && descriptor.getElement() == element;
    }

    private TreePath findTreePath(T element, DefaultMutableTreeNode start) {
        if (element == null
                || start == null) {
            return null;
        }
        Enumeration children = start.children();
        while (children.hasMoreElements()) {
            Object child = children.nextElement();
            if (!(child instanceof DefaultMutableTreeNode)) {
                continue;
            }
            if (hasElement(element, (DefaultMutableTreeNode) child)) {
                return new TreePath(((DefaultMutableTreeNode)child).getPath());
            }
            TreePath path = findTreePath(element, (DefaultMutableTreeNode) child);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    private boolean hasElement(T element, DefaultMutableTreeNode node) {
        NodeDescriptor descriptor = (NodeDescriptor) node.getUserObject();
        return descriptor != null && descriptor.getElement() == element;
    }


    @Override
    public void onAdded(T element) {
        invalidatePath(() -> getTreePath(getParentElement(element)));
    }

    @Override
    public void onModified(T element) {
        invalidatePath(() -> getTreePath(element));
    }

    @Override
    public void onRemoved(T element) {
        invalidatePath(() -> getTreePath(getParentElement(element)));
    }
}
