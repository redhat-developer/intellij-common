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

public class GettingStartedCourseBuilder {

    private GettingStartedCourse course;

    public GettingStartedCourseBuilder() { }

    public GettingStartedCourseBuilder createGettingStartedCourse(String title) {
        return createGettingStartedCourse(title, "");
    }

    public GettingStartedCourseBuilder createGettingStartedCourse(String title, String shortDescription) {
        return createGettingStartedCourse(title, shortDescription, null);
    }

    public GettingStartedCourseBuilder createGettingStartedCourse(String title, String shortDescription, URL feedbackUrl) {
        course = new GettingStartedCourse(title, shortDescription, feedbackUrl);
        return this;
    }

    public GettingStartedCourseBuilder withGroupLessons(GettingStartedGroupLessons group) {
        course.addGroupLessons(group);
        return this;
    }

    public GettingStartedCourse build() {
        return course;
    }

}
