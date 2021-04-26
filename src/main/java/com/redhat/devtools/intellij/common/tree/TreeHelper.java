/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.tree;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.util.ui.tree.TreeUtil;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TreeHelper {
    public static TreePath getPathForLocation(JTree tree, int x, int y) {
        TreePath path = tree.getClosestPathForLocation(x, y);
        Rectangle bounds = tree.getPathBounds(path);
        return bounds != null && bounds.y <= y && y < bounds.y + bounds.height ? path : null;
    }

    public static void addLinkSupport(final JTree tree) {
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = getPathForLocation(tree, e.getX(), e.getY());
                Object object = TreeUtil.getLastUserObject(path);
                if (object instanceof NodeDescriptor) {
                    object = ((NodeDescriptor<?>) object).getElement();
                }
                if (object instanceof LinkElement) {
                    ((LinkElement)object).execute();
                }
            }
        });
    }
}
