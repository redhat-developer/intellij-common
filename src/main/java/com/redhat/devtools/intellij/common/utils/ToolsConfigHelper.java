/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Util methods that deal with tool (oc, kubectl, odo, etc.) configs.
 */
public class ToolsConfigHelper {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    static ToolsConfig loadToolsConfig(URL url) throws IOException {
        try {
            return mapper.readValue(url, ToolsConfig.class);
        } catch (IOException e) {
            throw new IOException("Could not load tools config at " + url.toString() + ": " + e.getMessage(), e);
        }
    }
}
