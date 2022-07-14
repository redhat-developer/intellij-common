/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;

public class YAMLHelper {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final Pattern PATTERN_PROPERTY = Pattern.compile("(\\w+)(\\[(\\d)\\])*");

    /**
     * Retrieve value as String from YAML text
     *
     * @param yamlAsString Full YAML where to search the value from
     * @param path Path to scan to search for the value (e.g to get the resource name `String[] { "metadata", "name" } `)
     * @return last field value or null if the YAML doesn't contain any field
     * @throws IOException if erroring during parsing
     */
    public static String getStringValueFromYAML(String yamlAsString, String[] path) throws IOException {
        JsonNode nodeValue = YAMLHelper.getValueFromYAML(yamlAsString, path);
        if (nodeValue == null || !nodeValue.isTextual()) return null;
        return nodeValue.asText();
    }

    /**
     * Retrieve value as JsonNode from YAML text
     *
     * @param yamlAsString Full YAML where to search the value from
     * @param path Path to scan to search for the value (e.g to get the resource name `String[] { "metadata", "name" } `)
     * @return last field value or null if the YAML doesn't contain any field
     * @throws IOException if erroring during parsing
     */
    public static JsonNode getValueFromYAML(String yamlAsString, String[] path) throws IOException {
        if (yamlAsString == null) return null;
        JsonNode node = YAML_MAPPER.readTree(yamlAsString);
        for (String field: path) {
            Property property = createProperty(field, node);
            if (!property.existsIn(node)) {
                return null;
            }
            node = property.getNodeIn(node);
        }
        return node;
    }

    public static String JSONToYAML(JsonNode json) throws IOException {
        return JSONToYAML(json, true);
    }

    public static String JSONToYAML(JsonNode json, boolean minimizeQuotes) throws IOException {
        if (json == null) return "";
        try {
            return new YAMLMapper()
                    .configure(WRITE_DOC_START_MARKER, false)
                    .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, minimizeQuotes)
                    .writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }

    public static JsonNode YAMLToJsonNode(String yaml) throws IOException {
        return YAML_MAPPER.readTree(yaml);
    }

    public static JsonNode URLToJSON(URL file) throws IOException {
        if (file == null) {
            return null;
        }
        return YAML_MAPPER.readTree(file);
    }

    /**
     * Edit value of a yaml field and return the update yaml as JsonNode
     * @param yamlAsString original yaml to edit
     * @param fieldnames array of string to define path of field to change (e.g "spec", "template", "containers[0]", "image")
     * @param value new value to add to the field
     * @return edited yaml as JsonNode or null if unable to update yaml
     * @throws IOException if errored during yaml parsing
     */
    public static JsonNode editValueInYAML(String yamlAsString, String[] fieldnames, String value) throws IOException {
        if (yamlAsString == null) {
            return null;
        } else {
            JsonNode node = YAML_MAPPER.readTree(yamlAsString);
            JsonNode tmpNode = node;

            for (int i = 0; i < fieldnames.length; ++i) {
                Property property = createProperty(fieldnames[i], tmpNode);
                if (!property.existsIn(tmpNode)) {
                    return null;
                }
                if (i == fieldnames.length - 1) {
                    ((ObjectNode) tmpNode).put(property.getName(), value);
                } else {
                    tmpNode = property.getNodeIn(tmpNode);
                }
            }

            return node;
        }
    }

    /**
     * Add a label to resource metadata
     *
     * @param yaml the YAML resource configuration
     * @param labelKey label key to add
     * @param labelValue label value to add
     * @return The updated resource with the new label in the metadata
     * @throws IOException if errored during yaml parsing
     */
    public static JsonNode addLabelToResource(String yaml, String labelKey, String labelValue) throws IOException {
        ObjectNode resource = (ObjectNode) YAMLToJsonNode(yaml);
        ObjectNode metadata;
        if (!resource.has("metadata")) {
            metadata = YAML_MAPPER.createObjectNode();
        } else {
            metadata = (ObjectNode) resource.get("metadata");
        }

        if (!metadata.has("labels")) {
            ObjectNode newLabel = YAML_MAPPER.createObjectNode();
            newLabel.put(labelKey, labelValue);
            metadata.set("labels", newLabel);
        } else if (!metadata.get("labels").has(labelKey)) {
            ((ObjectNode)metadata.get("labels")).put(labelKey, labelValue);
        }

        resource.set("metadata", metadata);
        return resource;
    }

    /**
     * Remove a label from resource metadata
     * @param yaml the YAML resource configuration
     * @param labelKey label key to remove
     * @return the updated resource with the label removed
     * @throws IOException if errored during yaml parsing
     */
    public static JsonNode removeLabelFromResource(String yaml, String labelKey) throws IOException {
        ObjectNode resource = (ObjectNode) YAMLToJsonNode(yaml);
        ObjectNode metadata;
        if (!resource.has("metadata")) {
            metadata = YAML_MAPPER.createObjectNode();
        } else {
            metadata = (ObjectNode) resource.get("metadata");
        }

        if (metadata.has("labels") && metadata.get("labels").has(labelKey)) {
            ((ObjectNode) metadata.get("labels")).remove(labelKey);
            if (metadata.get("labels").size() == 0) {
                metadata.remove("labels");
            }
        }

        resource.set("metadata", metadata);
        return resource;
    }

    private static Property createProperty(String name, JsonNode node) {
        Property property = null;
        Matcher match = PATTERN_PROPERTY.matcher(name);
        if (match.matches()
                && match.group(3) != null) {
            property = new ArrayProperty(match.group(1), Integer.parseInt(match.group(3)));
        } else {
            property = new Property(name);
        }
        return property;
    }

    private static class Property {

        protected final String name;

        Property(String name) {
            this.name = name;
        }

        public boolean existsIn(JsonNode node) {
            return node != null
                    && node.has(name);
        }

        public JsonNode getNodeIn(JsonNode node) {
            if (node == null) {
                return null;
            }
            return node.get(name);
        }

        public String getName() {
            return name;
        }
    }

    private static class ArrayProperty extends Property {

        private final int index;

        ArrayProperty(String name, int index) {
            super(name);
            this.index = index;
        }

        public boolean existsIn(JsonNode node) {
            return node != null
                    && node.get(name) != null
                    && node.get(name).get(index) != null;
        }

        public JsonNode getNodeIn(JsonNode node) {
            if (node == null
                    || node.get(name) == null) {
                return null;
            }
            return node.get(name).get(index);
        }

    }

}
