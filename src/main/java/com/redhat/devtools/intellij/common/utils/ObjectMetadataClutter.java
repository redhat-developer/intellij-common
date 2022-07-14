/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ObjectMetadataClutter {
    private static final Logger logger = LoggerFactory.getLogger(ObjectMetadataClutter.class);

    /**
     * Properties in {@link io.fabric8.kubernetes.api.model.ObjectMeta} that are considered disposable clutter.
     */
    public static final List<String> properties = Arrays.asList(
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

    private static final String PROPERTY_METADATA = "metadata";

    /**
     * Removes clutter properties from the given content. Does nothing if the content is {@code null} or empty.
     *
     * @param resource where the clutter properties should be removed from
     * @return the content without the clutter properties
     */
    public static String remove(String resource) {
        if (resource == null
                || resource.isEmpty()) {
            return resource;
        }

        try {
            ObjectNode contentNode = (ObjectNode) YAMLHelper.YAMLToJsonNode(resource);
            ObjectNode metadata = contentNode.has(PROPERTY_METADATA) ? (ObjectNode) contentNode.get(PROPERTY_METADATA) : null;
            if (metadata != null) {
                metadata.remove(properties);
                contentNode.set(PROPERTY_METADATA, metadata);
                resource = YAMLHelper.JSONToYAML(contentNode);
            }
        } catch (IOException e) {
            logger.warn(e.getLocalizedMessage(), e);
        }
        return resource;
    }
}
