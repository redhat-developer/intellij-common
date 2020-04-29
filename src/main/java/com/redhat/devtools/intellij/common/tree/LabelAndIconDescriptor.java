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
import java.util.function.Function;

public class LabelAndIconDescriptor<T> extends PresentableNodeDescriptor<T> {

    private final T element;
    private final Function<T, String> labelProvider;
    private final Function<T, String> locationProvider;
    private final Icon nodeIcon;

    public LabelAndIconDescriptor(Project project, T element, Function<T, String> labelProvider, Function<T, String> locationProvider, Icon nodeIcon, @Nullable NodeDescriptor parentDescriptor) {
        super(project, parentDescriptor);
        this.element = element;
        this.labelProvider = labelProvider;
        this.locationProvider = locationProvider;
        this.nodeIcon = nodeIcon;
    }

    public LabelAndIconDescriptor(Project project, T element, Function<T, String> labelProvider, Icon nodeIcon, @Nullable NodeDescriptor parentDescriptor) {
        this(project, element, labelProvider, null, nodeIcon,parentDescriptor);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(labelProvider.apply(element));
        if (locationProvider != null) {
            presentation.setLocationString(locationProvider.apply(element));
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