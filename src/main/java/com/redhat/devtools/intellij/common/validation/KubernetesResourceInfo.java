/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.Objects;

public class KubernetesResourceInfo {

    private static final String KEY_METADATA = "metadata";
    private static final String KEY_NAME = "name";
    private static final String KEY_NAMESPACE = "namespace";

    private String name;
    private String namespace;
    private KubernetesTypeInfo typeInfo;

    public KubernetesResourceInfo(String name, String namespace, KubernetesTypeInfo info) {
        this.name = name;
        this.namespace = namespace;
        this.typeInfo = info;
    }

    public KubernetesResourceInfo() {
    }

    public static KubernetesResourceInfo extractMeta(PsiFile file) {
        KubernetesResourceInfo resourceInfo = new KubernetesResourceInfo();
        if (file instanceof JsonFile) {
            extractJsonMeta((JsonFile) file, resourceInfo);
        } else if (file instanceof YAMLFile) {
            extractYAMLMeta((YAMLFile) file, resourceInfo);
        }
        resourceInfo.setTypeInfo(KubernetesTypeInfo.extractMeta(file));
        return resourceInfo;
    }

    private static void extractJsonMeta(JsonFile file, KubernetesResourceInfo resourceInfo) {
        JsonValue content = file.getTopLevelValue();
        if (content == null) {
            return;
        }
        content.acceptChildren(new ResourceVisitor(resourceInfo));
    }

    private static void extractYAMLMeta(YAMLFile file, KubernetesResourceInfo resourceInfo) {
        if (file.getDocuments().isEmpty()) {
            return;
        }
        YAMLValue content = file.getDocuments().get(0).getTopLevelValue();
        if (content == null) {
            return;
        }
        content.acceptChildren(new ResourceVisitor(resourceInfo));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public KubernetesTypeInfo getTypeInfo() {
        return typeInfo;
    }

    public void setTypeInfo(KubernetesTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KubernetesResourceInfo that = (KubernetesResourceInfo) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(typeInfo, that.typeInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace, typeInfo);
    }

    @Override
    public String toString() {
        return name + ", " + namespace;
    }

    private static class ResourceVisitor extends PsiElementVisitor {

        private final KubernetesResourceInfo info;

        public ResourceVisitor(KubernetesResourceInfo info) {
            this.info = info;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            if (!(element instanceof PsiNamedElement)) {
                return;
            }
            PsiNamedElement namedElement = (PsiNamedElement) element;
            if (KEY_METADATA.equals(namedElement.getName())) {
                if (namedElement instanceof JsonProperty) {
                    ((JsonProperty) namedElement).getValue().acceptChildren(visitMetadata());
                } else if (namedElement instanceof YAMLKeyValue) {
                    ((YAMLKeyValue) namedElement).getValue().acceptChildren(visitMetadata());
                }
            }
        }

        @NotNull
        private PsiElementVisitor visitMetadata() {
            return new PsiElementVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if (!(element instanceof PsiNamedElement)) {
                        return;
                    }
                    PsiNamedElement namedElement = (PsiNamedElement) element;
                    if (KEY_NAME.equals(namedElement.getName())) {
                        setName(namedElement);
                    } else if (KEY_NAMESPACE.equals(namedElement.getName())) {
                        setNamespace(namedElement);
                    }
                }

                private void setName(PsiElement element) {
                    if (element instanceof JsonProperty) {
                        info.setName(valueOrNull((JsonProperty) element));
                    } else if (element instanceof YAMLKeyValue) {
                        info.setName(valueOrNull((YAMLKeyValue) element));
                    }
                }

                private void setNamespace(PsiElement element) {
                    if (element instanceof JsonProperty) {
                        info.setNamespace(valueOrNull((JsonProperty) element));
                    } else if (element instanceof YAMLKeyValue) {
                        info.setNamespace(valueOrNull((YAMLKeyValue) element));
                    }
                }

                private String valueOrNull(JsonProperty property) {
                    return property.getValue() != null ? property.getValue().getText() : null;
                }

                private String valueOrNull(YAMLKeyValue key) {
                    return key.getValueText() != null ? key.getValueText() : null;
                }

            };
        }
    }
}
