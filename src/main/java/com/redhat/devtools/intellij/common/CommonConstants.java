/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import java.util.Arrays;
import java.util.List;

public class CommonConstants {
    public static final String HOME_FOLDER = System.getProperty("user.home");
    public static final Key<Project> PROJECT = Key.create("com.redhat.devtools.intellij.common.project");
    public static final Key<Long> LAST_MODIFICATION_STAMP = Key.create("com.redhat.devtools.intellij.common.last.modification.stamp");
    public static final Key<Object> TARGET_NODE = Key.create("com.redhat.devtools.intellij.common.targetnode");
    public static final Key<String> CONTENT = Key.create("com.redhat.devtools.intellij.common.content");
    public static final Key<Boolean> CLEANED = Key.create("com.redhat.devtools.intellij.common.cleaned");

    /**
     * Properties in {@link io.fabric8.kubernetes.api.model.ObjectMeta} that are considered disposable clutter.
     *
     * @deprecated since 1.8.0, use {@link com.redhat.devtools.intellij.common.utils.ObjectMetadataClutter#properties} instead
     */
    @Deprecated
    public static final List<String> metadataClutter = Arrays.asList(
            "clusterName",
            "creationTimestamp",
            "deletionGracePeriodSeconds",
            "deletionTimestamp",
            "finalizers",
            "generation",
            "managedFields",
            "ownerReferences",
            "resourceVersion",
            "selfLink",
            "uid"
    );
}
