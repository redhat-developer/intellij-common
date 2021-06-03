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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class YAMLHelperTest {
    private static final String RESOURCE_PATH = "utils/YAMLHelper/";
    private static final String LABEL_KEY = "key";
    private static final String LABEL_VALUE = "value";

    @Test
    public void YamlToJsonNode_YAMLIsValid_JsonNode() throws IOException {
        String yaml = load(RESOURCE_PATH + "service.yaml");
        JsonNode result = YAMLHelper.YAMLToJsonNode(yaml);
        assertTrue(result.has("apiVersion"));
        assertEquals("serving.knative.dev/v1", result.get("apiVersion").asText());
        assertTrue(result.has("kind"));
        assertEquals("Service", result.get("kind").asText());
        assertTrue(result.has("metadata"));
        assertTrue(result.get("metadata").has("name"));
        assertEquals("test", result.get("metadata").get("name").asText());
        assertTrue(result.has("spec"));
    }

    @Test
    public void YamlToJsonNode_YAMLIsNotValid_Throws() {
        try {
            String yaml = load(RESOURCE_PATH + "invalid_service.yaml");
            YAMLHelper.YAMLToJsonNode(yaml);
        } catch (IOException e) {
            assertFalse(e.getLocalizedMessage().isEmpty());
        }
    }

    @Test
    public void AddLabelToResource_YAMLIsNotValid_Throws() {
        try {
            String yaml = load(RESOURCE_PATH + "invalid_service.yaml");
            YAMLHelper.addLabelToResource(yaml, LABEL_KEY, LABEL_VALUE);
        } catch (IOException e) {
            assertFalse(e.getLocalizedMessage().isEmpty());
        }
    }

    @Test
    public void AddLabelToResource_YAMLHasNotMetadataField_JsonNodeWithLabel() throws IOException {
        String yaml = load(RESOURCE_PATH + "missingmetadata_service.yaml");
        JsonNode result = YAMLHelper.addLabelToResource(yaml, LABEL_KEY, LABEL_VALUE);
        assertFalse(YAMLHelper.YAMLToJsonNode(yaml).has("metadata"));
        assertTrue(result.has("metadata"));
        JsonNode metadata = result.get("metadata");
        assertFalse(metadata.has("name"));
        assertTrue(metadata.has("labels"));
        assertTrue(metadata.get("labels").has(LABEL_KEY));
        assertEquals(LABEL_VALUE, metadata.get("labels").get(LABEL_KEY).asText());
    }

    @Test
    public void AddLabelToResource_YAMLHasMetadataFieldButNoLabels_JsonNodeWithLabel() throws IOException {
        String yaml = load(RESOURCE_PATH + "service.yaml");
        JsonNode result = YAMLHelper.addLabelToResource(yaml, LABEL_KEY, LABEL_VALUE);
        assertFalse(YAMLHelper.YAMLToJsonNode(yaml).get("metadata").has("labels"));
        assertTrue(result.has("metadata"));
        JsonNode metadata = result.get("metadata");
        assertTrue(metadata.has("name"));
        assertEquals("test", metadata.get("name").asText());
        assertTrue(metadata.has("labels"));
        assertTrue(metadata.get("labels").has(LABEL_KEY));
        assertEquals(LABEL_VALUE, metadata.get("labels").get(LABEL_KEY).asText());
    }

    @Test
    public void AddLabelToResource_YAMLHasMetadataFieldWithSomeLabels_JsonNodeWithLabels() throws IOException {
        String yaml = load(RESOURCE_PATH + "service_with_multiple_labels.yaml");
        JsonNode result = YAMLHelper.addLabelToResource(yaml, LABEL_KEY, LABEL_VALUE);
        assertTrue(YAMLHelper.YAMLToJsonNode(yaml).get("metadata").has("labels"));
        assertTrue(result.has("metadata"));
        JsonNode metadata = result.get("metadata");
        assertTrue(metadata.has("name"));
        assertEquals("test", metadata.get("name").asText());
        assertTrue(metadata.has("labels"));
        assertTrue(metadata.get("labels").has(LABEL_KEY));
        assertEquals(LABEL_VALUE, metadata.get("labels").get(LABEL_KEY).asText());
        assertTrue(metadata.get("labels").has("app"));
        assertEquals("v1", metadata.get("labels").get("app").asText());
        assertTrue(metadata.get("labels").has("sample"));
        assertEquals("foo", metadata.get("labels").get("sample").asText());
    }

    @Test
    public void AddLabelToResource_YAMLAlreadyHasLabel_OriginalJsonNode() throws IOException {
        String yaml = load(RESOURCE_PATH + "service_with_label.yaml");
        JsonNode result = YAMLHelper.addLabelToResource(yaml, LABEL_KEY, LABEL_VALUE);
        assertEquals(result, YAMLHelper.YAMLToJsonNode(yaml));
    }

    @Test
    public void RemoveLabelFromResource_YAMLIsNotValid_Throws() {
        try {
            String yaml = load(RESOURCE_PATH + "invalid_service.yaml");
            YAMLHelper.removeLabelFromResource(yaml, LABEL_KEY);
        } catch (IOException e) {
            assertFalse(e.getLocalizedMessage().isEmpty());
        }
    }

    @Test
    public void RemoveLabelFromResource_YAMLHasNoMetadataField_JsonNodeWithEmptyMetadata() throws IOException {
        String yaml = load(RESOURCE_PATH + "missingmetadata_service.yaml");
        JsonNode result = YAMLHelper.removeLabelFromResource(yaml, LABEL_KEY);
        assertFalse(YAMLHelper.YAMLToJsonNode(yaml).has("metadata"));
        assertTrue(result.has("metadata"));
        assertEquals(0, result.get("metadata").size());
    }

    @Test
    public void RemoveLabelFromResource_YAMLHasMetadataFieldButNoLabel_OriginalJsonNode() throws IOException {
        String yaml = load(RESOURCE_PATH + "service.yaml");
        JsonNode result = YAMLHelper.removeLabelFromResource(yaml, LABEL_KEY);
        assertEquals(result, YAMLHelper.YAMLToJsonNode(yaml));
    }

    @Test
    public void RemoveLabelFromResource_YAMLHasMetadataFieldWithLabel_JsonNodeWithoutLabel() throws IOException {
        String yaml = load(RESOURCE_PATH + "service_with_label.yaml");
        JsonNode result = YAMLHelper.removeLabelFromResource(yaml, LABEL_KEY);
        assertTrue(result.has("metadata"));
        JsonNode metadata = result.get("metadata");
        assertTrue(metadata.has("name"));
        assertEquals("test", metadata.get("name").asText());
        assertFalse(metadata.has("labels"));
        assertTrue(YAMLHelper.YAMLToJsonNode(yaml).get("metadata").get("labels").has(LABEL_KEY));
    }

    @Test
    public void GetStringValueFromYAML_YAMLHasNoField_Null() throws IOException {
        String yaml = load(RESOURCE_PATH + "service.yaml");
        String result = YAMLHelper.getStringValueFromYAML(yaml, new String[] { "fake" });
        assertNull(result);
    }

    @Test
    public void GetStringValueFromYAML_YAMLHasField_Value() throws IOException {
        String yaml = load(RESOURCE_PATH + "service.yaml");
        String result = YAMLHelper.getStringValueFromYAML(yaml, new String[] { "metadata", "name" });
        assertEquals("test", result);
    }

    @Test
    public void GetStringValueFromYAML_YAMLHasArrayField_ValuePerIndex() throws IOException {
        String yaml = load(RESOURCE_PATH + "service.yaml");
        String result = YAMLHelper.getStringValueFromYAML(yaml, new String[] { "spec", "template", "spec", "containers[0]", "image" });
        assertEquals("image", result);
    }

    @Test
    public void GetStringValueFromYAML_YAMLHasArrayFieldAndIndexIsOutOfRange_Null() throws IOException {
        String yaml = load(RESOURCE_PATH + "service.yaml");
        String result = YAMLHelper.getStringValueFromYAML(yaml, new String[] { "spec", "template", "spec", "containers[1]", "image" });
        assertNull(result);
    }

    @Test
    public void GetValueFromYAML_YAMLHasNoField_Null() throws IOException {
        String yaml = load(RESOURCE_PATH + "service.yaml");
        JsonNode result = YAMLHelper.getValueFromYAML(yaml, new String[] { "fake" });
        assertNull(result);
    }

    @Test
    public void GetValueFromYAML_YAMLHasField_Value() throws IOException {
        String yaml = load(RESOURCE_PATH + "service.yaml");
        JsonNode result = YAMLHelper.getValueFromYAML(yaml, new String[] { "metadata", "name" });
        assertEquals("test", result.asText());
    }

    @Test
    public void GetValueFromYAML_YAMLHasArrayField_ValuePerIndex() throws IOException {
        String yaml = load(RESOURCE_PATH + "service.yaml");
        JsonNode result = YAMLHelper.getValueFromYAML(yaml, new String[] { "spec", "template", "spec", "containers[0]", "image" });
        assertEquals("image", result.asText());
    }

    @Test
    public void GetValueFromYAML_YAMLHasArrayFieldAndIndexIsOutOfRange_Null() throws IOException {
        String yaml = load(RESOURCE_PATH + "service.yaml");
        JsonNode result = YAMLHelper.getValueFromYAML(yaml, new String[] { "spec", "template", "spec", "containers[1]", "image" });
        assertNull(result);
    }

    private String load(String name) throws IOException {
        return IOUtils.toString(YAMLHelperTest.class.getResource("/" + name), StandardCharsets.UTF_8);
    }
}
