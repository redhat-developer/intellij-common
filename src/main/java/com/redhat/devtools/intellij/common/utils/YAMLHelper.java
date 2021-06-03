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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;

public class YAMLHelper {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

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
        Pattern arrayPattern = Pattern.compile("(\\w+)(\\[(\\d)\\])*");
        for (String field: path) {
            int index = -1;
            Matcher match = arrayPattern.matcher(field);
            if (match.matches() && match.group(3) != null) {
                field = match.group(1);
                index = Integer.parseInt(match.group(3));
            }
            if (!node.has(field) ||
                    (index != -1 && !node.get(field).has(index))) {
                return null;
            }
            node = node.get(field);
            if (index != -1) {
                node = node.get(index);
            }
        }
        return node;
    }

    public static String JSONToYAML(JsonNode json) throws JsonProcessingException {
        if (json == null) return "";
        return new YAMLMapper().configure(WRITE_DOC_START_MARKER, false).writeValueAsString(json);
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
            Pattern arrayPattern = Pattern.compile("(\\w+)(\\[(\\d)\\])*");

            for(int i = 0; i < fieldnames.length; ++i) {
                String fieldname = fieldnames[i];
                int index = -1;
                Matcher match = arrayPattern.matcher(fieldname);
                if (match.matches() && match.group(3) != null) {
                    fieldname = match.group(1);
                    index = Integer.parseInt(match.group(3));
                }
                if (!tmpNode.has(fieldname) ||
                        (index != -1 && !tmpNode.get(fieldname).has(index))) {
                    return null;
                }

                if (i == fieldnames.length - 1) {
                    ((ObjectNode) tmpNode).put(fieldname, value);
                } else {
                    tmpNode = tmpNode.get(fieldname);
                    if (index != -1) {
                        tmpNode = tmpNode.get(index);
                    }
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
}
