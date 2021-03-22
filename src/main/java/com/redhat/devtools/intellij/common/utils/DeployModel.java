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
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

public class DeployModel {
    private String name, apiVersion, kind;
    private JsonNode spec;
    private CustomResourceDefinitionContext crdContext;

    public DeployModel(String name, String kind, String apiVersion, JsonNode spec, CustomResourceDefinitionContext crdContext) {
        this.name = name;
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.spec = spec;
        this.crdContext = crdContext;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public JsonNode getSpec() {
        return spec;
    }

    public void setSpec(JsonNode spec) {
        this.spec = spec;
    }

    public CustomResourceDefinitionContext getCrdContext() {
        return crdContext;
    }

    public void setCrdContext(CustomResourceDefinitionContext crdContext) {
        this.crdContext = crdContext;
    }
}
