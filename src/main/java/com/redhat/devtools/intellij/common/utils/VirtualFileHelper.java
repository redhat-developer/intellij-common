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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualFileHelper {
    private static final Logger logger = LoggerFactory.getLogger(VirtualFileHelper.class);

    public static VirtualFile createTempFile(String name, String content) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir"), name);
        if (file.exists()){
            file.delete();
            LocalFileSystem.getInstance().refreshIoFiles(Arrays.asList(file));
        }
        FileUtils.write(file, content, StandardCharsets.UTF_8);
        file.deleteOnExit();
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
    }

    public static String cleanContent(String content) {
        if (content.isEmpty()) {
            return content;
        }

        try {
            ObjectNode contentNode = (ObjectNode) YAMLHelper.YAMLToJsonNode(content);
            ObjectNode metadata = contentNode.has("metadata") ? (ObjectNode) contentNode.get("metadata") : null;
            if (metadata != null) {
                metadata.remove(Arrays.asList(
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
                ));
                contentNode.set("metadata", metadata);
                content = YAMLHelper.JSONToYAML(contentNode);
            }
        } catch (IOException e) {
            logger.warn(e.getLocalizedMessage(), e);
        }
        return content;
    }
}
