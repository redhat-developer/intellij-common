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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GettingStartedCourse {

    private final List<GettingStartedGroupLessons> groupLessons;
    private final String version;
    private final String title;
    private final String shortDescription;
    private final URL userRedirectForFeedback;

    public GettingStartedCourse(String version, String title, String shortDescription) {
        this(version, title, shortDescription, null);
    }

    public GettingStartedCourse(String version, String title, String shortDescription, URL userRedirectForFeedback) {
        this.version = version;
        this.title = title;
        this.shortDescription = shortDescription;
        this.groupLessons = new ArrayList<>();
        this.userRedirectForFeedback = userRedirectForFeedback;
    }

    public void addGroupLessons(GettingStartedGroupLessons group) {
        groupLessons.add(group);
    }

    public List<GettingStartedGroupLessons> getGroupLessons() {
        return groupLessons;
    }

    public String getTitle() {
        return title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public URL getUserRedirectForFeedback() {
        return userRedirectForFeedback;
    }

    public String getVersion() {
        return version;
    }
}
