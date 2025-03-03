/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.validation;

import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonValue;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.html.HtmlFileImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLValue;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.redhat.devtools.intellij.common.validation.KubernetesTypeInfo.fromFileName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class KubernetesTypeInfoTest {

    @Test
    public void getApiGroup_should_return_value_given_in_constructor() {
        // given
        String apiGroup = "apps/v1";

        // when
        KubernetesTypeInfo type = new KubernetesTypeInfo(apiGroup, null);

        // then
        assertThat(type.getApiGroup()).isEqualTo(apiGroup);
    }

    @Test
    public void getKind_should_return_value_given_in_constructor() {
        // given
        String kind = "Deployment";

        // when
        KubernetesTypeInfo type = new KubernetesTypeInfo(null, kind);

        // then
        assertThat(type.getKind()).isEqualTo(kind);
    }

    @Test
    public void getApiGroup_and_getKind_should_have_empty_instance_variables_when_default_constructor() {
        // when
        KubernetesTypeInfo type = new KubernetesTypeInfo();

        // then
        assertThat(type.getApiGroup()).isEmpty();
        assertThat(type.getKind()).isEmpty();
    }

    @Test
    public void setApiGroup_and_setKind_should_be_set_to_instance_variables() {
        // given
        KubernetesTypeInfo type = new KubernetesTypeInfo();
        String apiGroup = "apps/v1";
        String kind = "Deployment";

        // when
        type.setApiGroup(apiGroup);
        type.setKind(kind);

        // then
        assertThat(type.getApiGroup()).isEqualTo(apiGroup);
        assertThat(type.getKind()).isEqualTo(kind);
    }

    @Test
    public void equals_should_return_true_for_identical_apiGroup_and_kind() {
        // given
        KubernetesTypeInfo type1 = new KubernetesTypeInfo("apps/v1", "Deployment");
        KubernetesTypeInfo type2 = new KubernetesTypeInfo("apps/v1", "Deployment");

        // then
        assertThat(type1).isEqualTo(type2);
    }

    @Test
    public void equals_should_return_false_for_different_apiGroup_and_kind() {
        // given
        KubernetesTypeInfo type1 = new KubernetesTypeInfo("apps/v1", "Deployment");
        KubernetesTypeInfo type2 = new KubernetesTypeInfo("v1", "Pod");

        // then
        assertThat(type1).isNotEqualTo(type2);
    }

    @Test
    public void hashCode_should_be_equal_for_instances_with_identical_apiGroup_and_kind() {
        // given
        KubernetesTypeInfo type1 = new KubernetesTypeInfo("apps/v1", "Deployment");
        KubernetesTypeInfo type2 = new KubernetesTypeInfo("apps/v1", "Deployment");

        // then
        assertThat(type1.hashCode()).isEqualTo(type2.hashCode());
    }

    @Test
    public void toString_should_concatenate_apiGroup_and_kind() {
        // given
        KubernetesTypeInfo type = new KubernetesTypeInfo("apps/v1", "Deployment");

        // when
        String result = type.toString();

        // then
        assertThat(result).isEqualTo("apps/v1#Deployment");
    }

    @Test
    public void fromFilename_should_extract_apiGroup_and_kind_from_file_name() {
        assertThat(fromFileName("apps.v1_Deployment.yaml"))
                .isEqualTo(new KubernetesTypeInfo("apps.v1", "Deployment"));

        assertThat(fromFileName("Pod.yaml"))
                .isEqualTo(new KubernetesTypeInfo("", "Pod"));

        assertThat(fromFileName("apps.v1_Deployment"))
                .isEqualTo(new KubernetesTypeInfo("apps.v1", "Deployment"));
    }

    @Test
    public void create_should_return_null_for_file_that_is_neither_json_nor_yaml() {
        // given
        PsiFile mockFile = mock(HtmlFileImpl.class);

        // when
        KubernetesTypeInfo type = KubernetesTypeInfo.create(mockFile);

        // then
        assertThat(type).isNull();
    }

    @Test
    public void create_should_return_apiGroup_and_kind_from_json_file() {
        // given
        JsonFile jsonFile = mock(JsonFile.class);
        JsonValue topLevelValue = mock(JsonValue.class);
        doReturn(topLevelValue)
                .when(jsonFile).getTopLevelValue();

        JsonProperty apiVersion = mockJsonProperty("apiVersion","apps/v1");
        JsonProperty kind = mockJsonProperty("kind", "Deployment");
        visitJsonKubernetesTypeInfo(apiVersion, kind, topLevelValue);

        // when
        KubernetesTypeInfo type = KubernetesTypeInfo.create(jsonFile);

        // then
        assertThat(type).isNotNull();
        assertThat(type.getApiGroup()).isEqualTo("apps/v1");
        assertThat(type.getKind()).isEqualTo("Deployment");
    }

    @Test
    public void create_should_return_unquoted_apiGroup_and_kind_from_json_file() {
        // given
        JsonFile jsonFile = mock(JsonFile.class);
        JsonValue topLevelValue = mock(JsonValue.class);
        doReturn(topLevelValue)
                .when(jsonFile).getTopLevelValue();

        // name is never quoted, Psi always removes quotes. See JsonPsiImplUtils#getName.
        JsonProperty apiVersion = mockJsonProperty("apiVersion","\"apps/v1\"");
        JsonProperty kind = mockJsonProperty("kind", "\"Deployment\"");
        visitJsonKubernetesTypeInfo(apiVersion, kind, topLevelValue);

        // when
        KubernetesTypeInfo type = KubernetesTypeInfo.create(jsonFile);

        // then
        assertThat(type).isNotNull();
        assertThat(type.getApiGroup()).isEqualTo("apps/v1");
        assertThat(type.getKind()).isEqualTo("Deployment");
    }

    @Test
    public void create_should_return_apiGroup_and_kind_from_yaml_file() {
        // given
        YAMLFile yamlFile = mock(YAMLFile.class);
        YAMLDocument document = mock(YAMLDocument.class);
        when(yamlFile.getDocuments())
                .thenReturn(List.of(document));
        YAMLValue topLevelValue = mock(YAMLValue.class);
        when(document.getTopLevelValue())
                .thenReturn(topLevelValue);

        YAMLKeyValue apiVersion = mockYAMLKeyValue("apiVersion", "apps/v1");
        YAMLKeyValue kind = mockYAMLKeyValue("kind", "Deployment");
        visitYamlKubernetesTypeInfo(apiVersion, kind, topLevelValue);

        // when
        KubernetesTypeInfo type = KubernetesTypeInfo.create(yamlFile);

        // then
        assertThat(type).isNotNull();
        assertThat(type.getApiGroup()).isEqualTo("apps/v1");
        assertThat(type.getKind()).isEqualTo("Deployment");
    }

    @Test
    public void create_should_return_unquoted_apiGroup_and_kind_from_yaml_file() {
        // given
        YAMLFile yamlFile = mock(YAMLFile.class);
        YAMLDocument document = mock(YAMLDocument.class);
        when(yamlFile.getDocuments())
                .thenReturn(List.of(document));
        YAMLValue topLevelValue = mock(YAMLValue.class);
        when(document.getTopLevelValue())
                .thenReturn(topLevelValue);

        YAMLKeyValue apiVersion = mockYAMLKeyValue("apiVersion", "\"apps/v1\"");
        YAMLKeyValue kind = mockYAMLKeyValue("kind", "\"Deployment\"");
        visitYamlKubernetesTypeInfo(apiVersion, kind, topLevelValue);

        // when
        KubernetesTypeInfo type = KubernetesTypeInfo.create(yamlFile);

        // then
        assertThat(type).isNotNull();
        assertThat(type.getApiGroup()).isEqualTo("apps/v1");
        assertThat(type.getKind()).isEqualTo("Deployment");
    }

    @Test
    public void create_should_return_null_for_null_yaml_file() {
        // when
        KubernetesTypeInfo type = KubernetesTypeInfo.create((YAMLFile) null);

        // then
        assertThat(type).isNull();
    }

    @Test
    public void create_should_return_null_for_empty_yaml_file() {
        // given
        YAMLFile mockYamlFile = mock(YAMLFile.class);
        when(mockYamlFile.getDocuments())
                .thenReturn(Collections.emptyList());

        // when
        KubernetesTypeInfo type = KubernetesTypeInfo.create(mockYamlFile);

        // then
        assertThat(type).isNull();
    }

    @Test
    public void create_should_return_apiGroup_and_kind_for_yaml_document() {
        // given
        YAMLDocument document = mock(YAMLDocument.class);
        YAMLValue topLevelValue = mock(YAMLValue.class);
        when(document.getTopLevelValue())
                .thenReturn(topLevelValue);

        YAMLKeyValue apiVersion = mockYAMLKeyValue("apiVersion", "apps/v1");
        YAMLKeyValue kind = mockYAMLKeyValue("kind", "Deployment");

        visitYamlKubernetesTypeInfo(apiVersion, kind, topLevelValue);

        // when
        KubernetesTypeInfo type = KubernetesTypeInfo.create(document);

        // then
        assertThat(type).isNotNull();
        assertThat(type.getApiGroup()).isEqualTo("apps/v1");
        assertThat(type.getKind()).isEqualTo("Deployment");
    }

    @Test
    public void createTypes_should_return_null_for_null_yaml_document() {
        // when
        KubernetesTypeInfo type = KubernetesTypeInfo.create((YAMLDocument) null);

        // then
        assertThat(type).isNull();
    }

    @Test
    public void createTypesshould_return_null_for_null_yaml_file() {
        // when
        List<KubernetesTypeInfo> types = KubernetesTypeInfo.createTypes(null);

        // then
        assertThat(types).isNull();
    }

    @Test
    public void createTypesshould_return_null_for_empty_yaml_file() {
        // given
        YAMLFile mockYamlFile = mock(YAMLFile.class);
        when(mockYamlFile.getDocuments())
                .thenReturn(Collections.emptyList());

        // when
        List<KubernetesTypeInfo> types = KubernetesTypeInfo.createTypes(mockYamlFile);

        // then
        assertThat(types).isNull();
    }

    @Test
    public void createTypes_should_return_list_of_metas_for_yaml_file_with_multiple_documents() {
        // given
        YAMLFile yamlFile = mock(YAMLFile.class);
        YAMLDocument document1 = mock(YAMLDocument.class);
        YAMLDocument document2 = mock(YAMLDocument.class);
        when(yamlFile.getDocuments())
                .thenReturn(Arrays.asList(document1, document2));

        YAMLValue topLevelValue1 = mock(YAMLValue.class);
        when(document1.getTopLevelValue()).thenReturn(topLevelValue1);

        YAMLKeyValue apiVersion1 = mockYAMLKeyValue("apiVersion", "apps/v1");
        YAMLKeyValue kind1 = mockYAMLKeyValue("kind", "Deployment");
        visitYamlKubernetesTypeInfo(apiVersion1, kind1, topLevelValue1);

        YAMLValue topLevelValue2 = mock(YAMLValue.class);
        doReturn(topLevelValue2)
                .when(document2).getTopLevelValue();

        YAMLKeyValue apiVersion2 = mockYAMLKeyValue("apiVersion", "v1");
        YAMLKeyValue kind2 = mockYAMLKeyValue("kind", "Pod");
        visitYamlKubernetesTypeInfo(apiVersion2, kind2, topLevelValue2);

        // when
        List<KubernetesTypeInfo> types = KubernetesTypeInfo.createTypes(yamlFile);

        // then
        assertThat(types).hasSize(2);
        assertThat(types.get(0).getApiGroup()).isEqualTo("apps/v1");
        assertThat(types.get(0).getKind()).isEqualTo("Deployment");
        assertThat(types.get(1).getApiGroup()).isEqualTo("v1");
        assertThat(types.get(1).getKind()).isEqualTo("Pod");
    }

    private static @NotNull YAMLKeyValue mockYAMLKeyValue(String key, String value) {
        YAMLKeyValue keyValue = mock(YAMLKeyValue.class);
        when(keyValue.getKeyText())
                .thenReturn(key);
        when(keyValue.getValueText())
                .thenReturn(value);
        return keyValue;
    }

    private static @NotNull JsonProperty mockJsonProperty(String name, String value) {
        JsonProperty property = mock(JsonProperty.class);
        when(property.getName())
                .thenReturn(name);
        var jsonValue = mock(JsonValue.class);
        when(jsonValue.getText())
                .thenReturn(value);
        when(property.getValue())
                .thenReturn(jsonValue);
        return property;
    }

    private static void visitJsonKubernetesTypeInfo(JsonProperty apiVersion, JsonProperty kind, JsonValue topLevelValue) {
        doAnswer(invocation -> {
            KubernetesTypeInfo.JsonKubernetesTypeInfoVisitor visitor = invocation.getArgument(0);
            visitor.visitElement(apiVersion);
            visitor.visitElement(kind);
            return null;
        }).when(topLevelValue).acceptChildren(any(KubernetesTypeInfo.JsonKubernetesTypeInfoVisitor.class));
    }

    private static void visitYamlKubernetesTypeInfo(YAMLKeyValue apiVersion, YAMLKeyValue kind, YAMLValue topLevelValue) {
        doAnswer(invocation -> {
            KubernetesTypeInfo.YAMLKubernetesTypeInfoVisitor visitor = invocation.getArgument(0);
            visitor.visitElement(apiVersion);
            visitor.visitElement(kind);
            return null;
        }).when(topLevelValue)
                .acceptChildren(any(KubernetesTypeInfo.YAMLKubernetesTypeInfoVisitor.class));
    }

}