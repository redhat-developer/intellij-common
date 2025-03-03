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

import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLValue;

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

    public KubernetesTypeInfo() {}

    public static KubernetesTypeInfo create(PsiFile file) {
        if (file instanceof JsonFile) {
            return create((JsonFile) file);
        } else if (file instanceof YAMLFile) {
            return create((YAMLFile) file);
        } else {
            return null;
        }
    }

    private static KubernetesTypeInfo create(JsonFile file) {
        var collector = new JsonKubernetesTypeInfoVisitor();
        final JsonValue content = file.getTopLevelValue();
        if (content != null) {
            content.acceptChildren(collector);
        }
        return collector.getKubernetesTypeInfo();
    }

    /**
     * Extracts the k8s metadata of the first document in the given YAML file.
     *
     * @param file the yaml file to extract the k8s metadata of
     *
     * @return the k8s metadata of the first document in the given file
     */
    private static KubernetesTypeInfo create(YAMLFile file) {
        if (file == null
                || file.getDocuments().isEmpty()) {
            return null;
        }

        // only use the first document in the file
        return create(file.getDocuments().get(0));
    }

    /**
     * Creates a list of {@link KubernetesTypeInfo} for the given YAML file.
     * If the given file contains several documents then KubernetesTypeInfo's for each document will be created.
     * If there's only a single document only a single KuberenetesTypeInfo is created.
     * Returns {@code null} if the given file is {@code null} or empty.
     *
     * @param file the yaml file to create KubernetesTypeInfo's for
     *
     * @return KubernetesTypeInfos of all the documents in the given file
     */
    static List<KubernetesTypeInfo> createTypes(YAMLFile file) {
        if (file == null
                || file.getDocuments().isEmpty()) {
            return null;
        }

        return file.getDocuments().stream()
                .map(KubernetesTypeInfo::create)
                .toList();
    }

    public static KubernetesTypeInfo create(YAMLDocument document) {
        final KubernetesTypeInfoVisitor collector = new YAMLKubernetesTypeInfoVisitor();
        if (document != null) {
            final YAMLValue content = document.getTopLevelValue();
            if (content != null) {
                content.acceptChildren(collector);
            }
        }
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
            if (element instanceof YAMLKeyValue property) {
                String value = StringUtil.unquoteString(property.getValueText());
                if (property.getKeyText().equals(KEY_API_VERSION)) {
                    setApiGroup(value);
                } else if (property.getKeyText().equals(KEY_KIND)) {
                    setKind(value);
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
                String value = StringUtil.unquoteString(property.getValue().getText());
                if (property.getName().equals(KEY_API_VERSION)) {
                    setApiGroup(value);
                } else if (property.getName().equals(KEY_KIND)) {
                    setKind(value);
                }
            }
        }
    }

}
