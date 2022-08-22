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

    public GettingStartedCourseBuilder createGettingStartedCourse(String version, String title) {
        return createGettingStartedCourse(version, title, "");
    }

    public GettingStartedCourseBuilder createGettingStartedCourse(String version, String title, String shortDescription) {
        return createGettingStartedCourse(version, title, shortDescription, null);
    }

    public GettingStartedCourseBuilder createGettingStartedCourse(String version, String title, String shortDescription, URL feedbackUrl) {
        course = new GettingStartedCourse(version, title, shortDescription, feedbackUrl);
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
