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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GettingStartedGroupLessons {

    List<GettingStartedLesson> lessons;
    String title, shortDescription;

    public GettingStartedGroupLessons(String title, String shortDescription, GettingStartedLesson... lessons) {
        this.title = title;
        this.shortDescription = shortDescription;
        this.lessons = new ArrayList<>(Arrays.asList(lessons));
    }

    public void addLesson(GettingStartedLesson lesson) {
        lessons.add(lesson);
    }

    public List<GettingStartedLesson> getLessons() {
        return lessons;
    }

    public String getTitle() {
        return title;
    }

    public String getShortDescription() {
        return shortDescription;
    }
}
