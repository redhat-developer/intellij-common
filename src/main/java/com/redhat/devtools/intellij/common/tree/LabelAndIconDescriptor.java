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

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class LabelAndIconDescriptor<T> extends PresentableNodeDescriptor<T> {

    private final T element;
    private final String label;
    private final String location;
    private final Icon nodeIcon;

    public LabelAndIconDescriptor(Project project, T element, String label, String location, Icon nodeIcon, @Nullable NodeDescriptor parentDescriptor) {
        super(project, parentDescriptor);
        this.element = element;
        this.label = label;
        this.location = location;
        this.nodeIcon = nodeIcon;
    }

    public LabelAndIconDescriptor(Project project, T element, String label, Icon nodeIcon, @Nullable NodeDescriptor parentDescriptor) {
        this(project, element, label, null, nodeIcon,parentDescriptor);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(label);
        if (location != null) {
            presentation.setLocationString(location);
        }
        if (nodeIcon != null) {
            presentation.setIcon(nodeIcon);
        }
    }

    @Override
    public T getElement() {
        return element;
    }
}