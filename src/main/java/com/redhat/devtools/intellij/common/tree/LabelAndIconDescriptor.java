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
import java.util.function.Supplier;

public class LabelAndIconDescriptor<T> extends PresentableNodeDescriptor<T> {

    private final T element;
    private final Supplier<String> label;
    private final Supplier<String> location;
    private final Supplier<Icon> nodeIcon;

    public LabelAndIconDescriptor(Project project, T element, String label, String location, Icon nodeIcon, @Nullable NodeDescriptor parentDescriptor) {
        super(project, parentDescriptor);
        this.element = element;
        this.label = () -> label;
        this.location = () -> location;
        this.nodeIcon = () -> nodeIcon;
    }

    public LabelAndIconDescriptor(Project project, T element, String label, Icon nodeIcon, @Nullable NodeDescriptor parentDescriptor) {
        this(project, element, label, null, nodeIcon,parentDescriptor);
    }

    public LabelAndIconDescriptor(Project project, T element, Supplier<String> label, Supplier<String> location, Supplier<Icon> nodeIcon, @Nullable NodeDescriptor parentDescriptor) {
        super(project, parentDescriptor);
        this.element = element;
        this.label = label;
        this.location = location;
        this.nodeIcon = nodeIcon;
    }

    public LabelAndIconDescriptor(Project project, T element, Supplier<String> label, Supplier<Icon> nodeIcon, @Nullable NodeDescriptor parentDescriptor) {
        this(project, element, label, null, nodeIcon,parentDescriptor);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(label.get());
        if (location != null && location.get() != null) {
            presentation.setLocationString(location.get());
        }
        if (nodeIcon != null && nodeIcon.get() != null) {
            presentation.setIcon(nodeIcon.get());
        }
    }

    @Override
    public T getElement() {
        return element;
    }
}