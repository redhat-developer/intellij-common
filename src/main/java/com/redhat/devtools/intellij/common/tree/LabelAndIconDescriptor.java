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
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LabelAndIconDescriptor<T> extends PresentableNodeDescriptor<T> {

    public static final Pattern HREF_PATTERN = Pattern.compile("<a(?:\\s+href\\s*=\\s*[\"']([^\"']*)[\"'])?\\s*>([^<]*)</a>");

    public static final SimpleTextAttributes LINK_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_UNDERLINE | SimpleTextAttributes.STYLE_CLICKABLE, JBColor.blue);

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
        processLabel(presentation);
        if (location != null && location.get() != null) {
            presentation.setLocationString(location.get());
        }
        if (nodeIcon != null && nodeIcon.get() != null) {
            presentation.setIcon(nodeIcon.get());
        }
    }

    private void processLabel(@NotNull PresentationData presentation) {
        String text = label.get();
        Matcher matcher = HREF_PATTERN.matcher(text);
        if (matcher.find()) {
            int prev = 0;
            do {
                if (matcher.start() != prev) {
                    presentation.addText(text.substring(prev, matcher.start()), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }
                presentation.addText(matcher.group(2), LINK_ATTRIBUTES);
                prev = matcher.end();
            }
            while (matcher.find());

            if (prev < text.length()) {
                presentation.addText(text.substring(prev), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }

        } else {
            presentation.setPresentableText(label.get());
        }
    }

    @Override
    public T getElement() {
        return element;
    }
}