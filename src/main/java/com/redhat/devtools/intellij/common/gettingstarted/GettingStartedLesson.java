/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.gettingstarted;

import javax.swing.Action;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class GettingStartedLesson {

    private final String title;
    private final String description;
    private final List<Action> actions;
    private final URL gif;

    public GettingStartedLesson(String title) {
        this(title, "", Collections.emptyList(), null);
    }

    public GettingStartedLesson(String title, String description, List<Action> actions, URL gif) {
        this.title = title;
        this.description = description;
        this.actions = actions;
        this.gif = gif;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<Action> getActions() {
        return actions;
    }

    public URL getGif() {
        return gif;
    }
}
