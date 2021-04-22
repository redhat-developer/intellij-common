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
package com.redhat.devtools.intellij.common.utils.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.redhat.devtools.intellij.common.model.GenericResource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GenericResourceDeserializerTest {

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper(new YAMLFactory());
    }

    @Test
    public void Convert_ResourceHasNoName_Throws() {
        try {
            String yaml = load("resource_without_name.yaml");
            mapper.readValue(yaml, GenericResource.class);
        } catch (IOException e) {
            assertTrue(e.getLocalizedMessage().contains("Resource configuration not valid. The name is missing or invalid."));
        }
    }

    @Test
    public void Convert_ResourceHasNoKind_Throws() {
        try {
            mapper.readValue(load("resource_without_kind.yaml"), GenericResource.class);
        } catch (IOException e) {
            assertTrue(e.getLocalizedMessage().contains("Resource configuration not valid. The kind is missing or invalid."));
        }
    }

    @Test
    public void Convert_ResourceHasNoApiVersion_Throws() {
        try {
            mapper.readValue(load("resource_without_apiversion.yaml"), GenericResource.class);
        } catch (IOException e) {
            assertTrue(e.getLocalizedMessage().contains("Resource configuration not valid. ApiVersion is missing or invalid."));
        }
    }

    @Test
    public void Convert_ResourceHasNoMetadataSection_Throws() {
        try {
            mapper.readValue(load("resource_without_metadata.yaml"), GenericResource.class);
        } catch (IOException e) {
            assertTrue(e.getLocalizedMessage().contains("Resource configuration not valid. Metadata field is missing or invalid."));
        }
    }

    @Test
    public void Convert_ResourceHasNoSpec_Throws() {
        try {
            mapper.readValue(load("resource_without_spec.yaml"), GenericResource.class);
        } catch (IOException e) {
            assertTrue(e.getLocalizedMessage().contains("Resource configuration not valid. Spec field is missing or invalid."));
        }
    }

    @Test
    public void Convert_ResourceIsValid_GenericResource() throws IOException {
        GenericResource genericResource = mapper.readValue(load("resource.yaml"), GenericResource.class);
        assertEquals(genericResource.getApiVersion(), "tekton.dev/v1beta1");
        assertEquals(genericResource.getKind(), "Pipeline");
        assertEquals(genericResource.getName(), "foo");
        assertTrue(genericResource.getMetadata() != null);
        assertTrue(genericResource.getSpec() != null);
    }

    private String load(String name) throws IOException {
        return IOUtils.toString(GenericResourceDeserializerTest.class.getResource("/model/" + name), StandardCharsets.UTF_8);
    }
}
