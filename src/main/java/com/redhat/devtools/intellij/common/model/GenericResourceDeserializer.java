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
package com.redhat.devtools.intellij.common.model;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;

public class GenericResourceDeserializer extends StdNodeBasedDeserializer<GenericResource> {
    public GenericResourceDeserializer() {
        super(GenericResource.class);
    }

    @Override
    public GenericResource convert(JsonNode root, DeserializationContext ctxt) throws JsonMappingException {
        if (!root.has("apiVersion") || !root.hasNonNull("apiVersion")) {
            throw new JsonMappingException(ctxt.getParser(), "Resource configuration not valid. ApiVersion is missing or invalid.");
        }
        if (!root.has("kind") || !root.hasNonNull("kind")) {
            throw new JsonMappingException(ctxt.getParser(), "Resource configuration not valid. The kind is missing or invalid.");
        }
        if (!root.has("metadata") || !root.hasNonNull("metadata")) {
            throw new JsonMappingException(ctxt.getParser(), "Resource configuration not valid. Metadata field is missing or invalid.");
        }
        JsonNode metadata = root.get("metadata");
        if ((!metadata.has("name") || !metadata.hasNonNull("name"))
                && (!metadata.has("generateName") || !metadata.hasNonNull("generateName"))) {
            throw new JsonMappingException(ctxt.getParser(), "Resource configuration not valid. The name is missing or invalid.");
        }

        if (!root.has("spec") || !root.hasNonNull("spec")) {
            throw new JsonMappingException(ctxt.getParser(), "Resource configuration not valid. Spec field is missing or invalid.");
        }
        return new GenericResource(root.get("apiVersion").asText(), root.get("kind").asText(), metadata, root.get("spec"));
    }
}
