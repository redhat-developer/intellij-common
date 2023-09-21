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
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.tree.TreePathUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
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
            } else {
                treeModel.invalidate(path, true);
            }
        });
    }

    private void invalidateRoot() {
        treeModel.invalidate();
    }

    private T getParentElement(T element) {
        return (T) structure.getParentElement(element);
    }

    protected TreePath getTreePath(T element) {
        if (element == null) {
            return null;
        }
        TreeNode node = getNode(element);
        if (node != null) {
            return new TreePath(node);
        } else {
            return new TreePath(treeModel.getRoot());
        }
    }

    @Nullable
    private TreeNode getNode(T element) {
        TreeNode node;
        if (isRootNode(element)) {
            node = treeModel.getRoot();
        } else {
            node = findTreeNode(element, (DefaultMutableTreeNode)treeModel.getRoot());
        }
        return node;
    }

    protected boolean isRootNode(T element) {
        TreeNode root = treeModel.getRoot();
        if (!(root instanceof DefaultMutableTreeNode)) {
            return false;
        }
        Object userObject = ((DefaultMutableTreeNode) root).getUserObject();
        if (!(userObject instanceof NodeDescriptor) ){
            return false;
        }
        NodeDescriptor<?> descriptor = (NodeDescriptor<?>) userObject;
        return descriptor.getElement() == element;
    }

    private TreeNode findTreeNode(T element, DefaultMutableTreeNode start) {
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
                return (DefaultMutableTreeNode) child;
            }
            TreeNode node = findTreeNode(element, (DefaultMutableTreeNode) child);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    private boolean hasElement(T element, DefaultMutableTreeNode node) {
        NodeDescriptor<?> descriptor = (NodeDescriptor) node.getUserObject();
        return descriptor != null && descriptor.getElement() == element;
    }


    @Override
    public void onAdded(T element) {
        treeModel.getInvoker().invokeLater(() ->
            invalidatePath(() -> getTreePath(getParentElement(element)))
        );
    }

    @Override
    public void onModified(T element) {
        treeModel.getInvoker().invokeLater(() -> {
            TreePath path = getTreePath(element);
            if (path == null) {
                return;
            }
            updateDescriptor(path);
            invalidatePath(() -> path);
        });
    }

    private void updateDescriptor(TreePath path) {
        TreeNode node = TreePathUtil.toTreeNode(path);
        if (!(node instanceof DefaultMutableTreeNode)) {
            return;
        }
        Object userObject = ((DefaultMutableTreeNode) node).getUserObject();
        if (!(userObject instanceof PresentableNodeDescriptor)) {
            return;
        }

        ((PresentableNodeDescriptor<?>) userObject).update();
    }

    @Override
    public void onRemoved(T element) {
        treeModel.getInvoker().invokeLater(() ->
           invalidatePath(() -> getTreePath(getParentElement(element)))
        );
    }
}
