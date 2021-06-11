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

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.devtools.intellij.common.BaseTest;
import java.io.IOException;
import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class VirtualFileHelperTest extends BaseTest {

    private static final String RESOURCE_PATH = "utils/virtualFileHelper/";

    @Test
    public void CleanContent_ContentIsEmpty_OriginalContent() {
        String result = VirtualFileHelper.cleanContent("");
        assertEquals("", result);
    }

    @Test
    public void CleanContent_ContentHasNoMetadata_OriginalContent() throws IOException {
        String content = load(RESOURCE_PATH + "pipeline_without_metadata.yaml");
        String result = VirtualFileHelper.cleanContent(content);
        assertEquals(content, result);
    }

    @Test
    public void CleanContent_ContentHasMetadataWithoutClutterTags_OriginalContent() throws IOException {
        String content = load(RESOURCE_PATH + "pipeline_with_no_clutters.yaml");
        JsonNode content_Node = YAMLHelper.YAMLToJsonNode(content);
        String result = VirtualFileHelper.cleanContent(content);
        JsonNode result_Node = YAMLHelper.YAMLToJsonNode(result);
        assertEquals(content_Node.get("metadata"), result_Node.get("metadata"));
        assertEquals(content_Node.get("spec"), result_Node.get("spec"));
    }

    @Test
    public void CleanContent_ContentHasMetadataWithoutClutterTags_CleanedContent() throws IOException {
        String content = load(RESOURCE_PATH + "pipeline_with_clutters.yaml");
        String content_without_clutters = load(RESOURCE_PATH + "pipeline_with_no_clutters.yaml");
        JsonNode content_without_clutters_Node = YAMLHelper.YAMLToJsonNode(content_without_clutters);
        String result = VirtualFileHelper.cleanContent(content);
        JsonNode result_Node = YAMLHelper.YAMLToJsonNode(result);
        assertEquals(content_without_clutters_Node.get("metadata"), result_Node.get("metadata"));
        assertEquals(content_without_clutters_Node.get("spec"), result_Node.get("spec"));
    }
}
