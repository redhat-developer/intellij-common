/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.validation;

import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonElement;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonProperty;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLPsiElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class KubernetesTypeInfo {

    private static final String KEY_API_VERSION = "apiVersion";
    private static final String KEY_KIND = "kind";

    private String apiGroup = "";
    private String kind = "";

    public KubernetesTypeInfo(String apiGroup, String kind) {
        this.apiGroup = apiGroup;
        this.kind = kind;
    }

    KubernetesTypeInfo() {}

    public static KubernetesTypeInfo create(PsiElement element) {
        if (element instanceof JsonElement jsonElement) {
            return create(jsonElement);
        } else if (element instanceof YAMLPsiElement yamlElement) {
            return create(yamlElement);
        } else {
            return null;
        }
    }

    private static @Nullable KubernetesTypeInfo create(YAMLPsiElement yamlElement) {
        if (yamlElement instanceof YAMLFile yamlFile) {
            return create(yamlFile);
        } else if (yamlElement instanceof YAMLDocument yamlDocument) {
            return create(yamlDocument);
        } else {
            return collect(yamlElement);
        }
    }

    private static @Nullable KubernetesTypeInfo create(JsonElement jsonElement) {
        if (jsonElement instanceof JsonFile jsonFile) {
            return create(jsonFile);
        } else if (jsonElement instanceof JsonArray jsonArray) {
            return create(jsonArray);
        } else {
            return collect(jsonElement);
        }
    }

    public static KubernetesTypeInfo create(JsonFile file) {
        if (file == null) {
            return null;
        }
        var topLevelValue = file.getTopLevelValue();
        if (topLevelValue instanceof JsonArray) {
            return create(topLevelValue);
        } else {
            return create(topLevelValue);
        }
    }

    private static KubernetesTypeInfo create(JsonArray array) {
        if (array == null
                || array.getChildren().length == 0) {
            return null;
        }

        return create(array.getChildren()[0]);
    }

    private static KubernetesTypeInfo collect(JsonElement element) {
        if (element == null) {
            return null;
        }
        var collector = new JsonKubernetesTypeInfoVisitor();
        element.acceptChildren(collector);
        return collector.getKubernetesTypeInfo();
    }

    public static List<KubernetesTypeInfo> createTypes(PsiFile file) {
        if (file instanceof JsonFile jsonFile) {
            return createTypes(jsonFile);
        } else if (file instanceof YAMLFile yamlFile) {
            return createTypes(yamlFile);
        } else {
            return null;
        }
    }

    private static List<KubernetesTypeInfo> createTypes(JsonFile file) {
        if (file == null) {
            return null;
        }

        var topLevelValue = file.getTopLevelValue();
        if (topLevelValue instanceof JsonArray array) {
            return createTypes(array);
        } else {
            return createTypes(topLevelValue);
        }
    }

    private static List<KubernetesTypeInfo> createTypes(JsonArray array) {
        if (array == null) {
            return null;
        }
        return Arrays.stream(array.getChildren())
                .map(KubernetesTypeInfo::create)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<KubernetesTypeInfo> createTypes(JsonElement element) {
        if (element == null) {
            return null;
        }
        return Collections.singletonList(create(element));
    }

    /**
     * Creates a list of {@link KubernetesTypeInfo} for the given YAML file.
     * If the given file contains several documents then KubernetesTypeInfo's for each document will be created.
     * Returns {@code null} if the given file is {@code null} or has no documents.
     *
     * @param file the yaml file to create KubernetesTypeInfo's for
     *
     * @return KubernetesTypeInfos of all the documents in the given file
     */
    static List<KubernetesTypeInfo> createTypes(YAMLFile file) {
        if (file == null
                || file.getDocuments() == null) {
            return null;
        }

        return file.getDocuments().stream()
                .map(KubernetesTypeInfo::create)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Extracts the k8s metadata of the first document in the given YAML file.
     *
     * @param file the yaml file to extract the k8s metadata of
     *
     * @return the k8s metadata of the first document in the given file
     */
    public static KubernetesTypeInfo create(YAMLFile file) {
        if (file == null
                || file.getDocuments().isEmpty()) {
            return null;
        }

        // only use the first document in the file
        return create(file.getDocuments().get(0));
    }

    public static KubernetesTypeInfo create(YAMLDocument document) {
        YAMLPsiElement element = null;
        if (document != null) {
            element = document.getTopLevelValue();
        }
        return collect(element);
    }

    private static KubernetesTypeInfo collect(YAMLPsiElement element) {
        if (element == null) {
            return null;
        }
        final KubernetesTypeInfoVisitor collector = new YAMLKubernetesTypeInfoVisitor();
        element.acceptChildren(collector);
        return collector.getKubernetesTypeInfo();
    }

    public String getApiGroup() {
        return apiGroup;
    }

    public void setApiGroup(String apiGroup) {
        this.apiGroup = apiGroup;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KubernetesTypeInfo that = (KubernetesTypeInfo) o;
        return Objects.equals(apiGroup, that.apiGroup) &&
                Objects.equals(kind, that.kind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiGroup, kind);
    }

    @Override
    public String toString() {
        return apiGroup + '#' + kind;
    }

    public static KubernetesTypeInfo fromFileName(String filename) {
        int index = filename.indexOf('_');
        String apiGroup = (index != (-1)) ? filename.substring(0, index) : "";
        String kind = (index != (-1)) ? filename.substring(index + 1) : filename;
        index = kind.lastIndexOf('.');
        kind = (index != (-1)) ? kind.substring(0, index) : kind;
        return new KubernetesTypeInfo(apiGroup, kind);
    }

    private static abstract class KubernetesTypeInfoVisitor extends PsiElementVisitor {

        private KubernetesTypeInfo info = null;

        protected void setApiGroup(String apiGroup) {
            existingOrCreate().setApiGroup(apiGroup);
        }

        protected void setKind(String kind) {
            existingOrCreate().setKind(kind);
        }

        private KubernetesTypeInfo existingOrCreate() {
            if (info == null) {
                this.info = new KubernetesTypeInfo();
            }
            return info;
        }

        public KubernetesTypeInfo getKubernetesTypeInfo() {
            return info;
        }
    }

    static class YAMLKubernetesTypeInfoVisitor extends KubernetesTypeInfoVisitor {
        @Override
        public void visitElement(@NotNull PsiElement element) {
            if (element instanceof YAMLKeyValue keyValue) {
                var keyText = keyValue.getKeyText();
                switch (keyText) {
                    case KEY_API_VERSION ->
                            setApiGroup(StringUtil.unquoteString(keyValue.getValueText()));
                    case KEY_KIND ->
                            setKind(StringUtil.unquoteString(keyValue.getValueText()));
                }
            }
        }
    }

    static class JsonKubernetesTypeInfoVisitor extends KubernetesTypeInfoVisitor {
        @Override
        public void visitElement(@NotNull PsiElement element) {
            if (element instanceof JsonProperty property
                    && property.getValue() != null
                    && property.getValue().getText() != null) {
                var name = property.getName();
                switch(name) {
                    case KEY_API_VERSION ->
                            setApiGroup(StringUtil.unquoteString(property.getValue().getText()));
                    case KEY_KIND ->
                            setKind(StringUtil.unquoteString(property.getValue().getText()));
                }
            }
        }
    }

}
