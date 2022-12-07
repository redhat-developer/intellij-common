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
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class MetadataClutter {
    private static final Logger logger = LoggerFactory.getLogger(MetadataClutter.class);

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

    private static final List<Consumer<ObjectMeta>> setters = Arrays.asList(
            metadata -> {
                if (metadata.getAdditionalProperties() != null) {
                    metadata.getAdditionalProperties().remove("clusterName");
                }
            },
            metadata -> metadata.setCreationTimestamp(null),
            metadata -> metadata.setDeletionGracePeriodSeconds(null),
            metadata -> metadata.setDeletionTimestamp(null),
            metadata -> metadata.setFinalizers(Collections.emptyList()),
            metadata -> metadata.setGeneration(null),
            metadata -> metadata.setManagedFields(Collections.emptyList()),
            metadata -> metadata.setOwnerReferences(Collections.emptyList()),
            metadata -> metadata.setResourceVersion(null),
            metadata -> metadata.setSelfLink(null),
            metadata -> metadata.setUid(null)
    );

    private static final String PROPERTY_METADATA = "metadata";

    /**
     * Removes clutter properties from the given textual content. Does nothing if the content is {@code null} or empty.
     *
     * @param resource where the clutter properties should be removed from
     * @return the content without the clutter properties
     */
    public static String remove(String resource) {
        return remove(resource, true);
    }

    /**
     * Removes clutter properties from the given textual content. Does nothing if the content is {@code null} or empty.
     *
     * @param resource where the clutter properties should be removed from
     * @param minimizeQuotes if, when converting from json to yaml, quotes should be removed if not strictly needed
     * @return the content without the clutter properties
     */
    public static String remove(String resource, boolean minimizeQuotes) {
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
                resource = YAMLHelper.JSONToYAML(contentNode, minimizeQuotes);
            }
        } catch (IOException e) {
            logger.warn(e.getLocalizedMessage(), e);
        }
        return resource;
    }

    /**
     * Removes clutter properties from the given {@link ObjectMeta} content. Does nothing if the content is {@code null}.
     *
     * @param metadata where the clutter properties should be removed from
     * @return the metadata without the clutter properties
     */
    public static ObjectMeta remove(ObjectMeta metadata) {
        if (metadata == null) {
            return null;
        }

        setters.forEach(consumer -> consumer.accept(metadata));
        return metadata;
    }

}
