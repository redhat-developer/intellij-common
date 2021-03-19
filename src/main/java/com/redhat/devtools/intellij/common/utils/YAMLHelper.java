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

    public static String getStringValueFromYAML(String yamlAsString, String[] fieldnames) throws IOException {
        JsonNode nodeValue = YAMLHelper.getValueFromYAML(yamlAsString, fieldnames);
        if (nodeValue == null || !nodeValue.isTextual()) return null;
        return nodeValue.asText();
    }

    public static JsonNode getValueFromYAML(String yamlAsString, String[] fieldnames) throws IOException {
        if (yamlAsString == null) return null;
        JsonNode node = YAML_MAPPER.readTree(yamlAsString);
        for (String fieldname: fieldnames) {
            if (!node.has(fieldname)) return null;
            node = node.get(fieldname);
        }
        return node;
    }

    public static String JSONToYAML(JsonNode json) throws JsonProcessingException {
        if (json == null) return "";
        return new YAMLMapper().configure(WRITE_DOC_START_MARKER, false).writeValueAsString(json);
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
}
