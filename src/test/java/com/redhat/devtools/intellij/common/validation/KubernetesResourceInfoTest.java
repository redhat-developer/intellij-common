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

import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesResourceInfoTest {

    private static final String KEY_METADATA = "metadata";
    private static final String KEY_NAME = "name";
    private static final String KEY_NAMESPACE = "namespace";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private JsonFile jsonFile;

    @Mock
    private YAMLFile yamlFile;

    @Mock
    private JsonValue jsonTopLevelValue;

    @Mock
    private YAMLDocument yamlDocument;

    @Mock
    private YAMLValue yamlTopLevelValue;

    @Mock
    private KubernetesTypeInfo typeInfo;

    private MockedStatic<KubernetesTypeInfo> mockStatic;

    @Before
    public void setUp() {
        // Registering a static mock for UserService before each test
        mockStatic = mockStatic(KubernetesTypeInfo.class);
    }

    @After
    public void tearDown() {
        // Closing the mockStatic after each test
        mockStatic.close();
    }

    @Test
    public void create_returns_info_for_json_file() {
        // given
        JsonProperty metadataProperty = mock(JsonProperty.class);
        JsonValue metadataValue = mock(JsonValue.class);
        JsonProperty nameProperty = mock(JsonProperty.class);
        JsonProperty namespaceProperty = mock(JsonProperty.class);
        JsonValue nameValue = mock(JsonValue.class);
        JsonValue namespaceValue = mock(JsonValue.class);

        when(jsonFile.getTopLevelValue())
                .thenReturn(jsonTopLevelValue);
        when(metadataProperty.getName())
                .thenReturn(KEY_METADATA);
        when(metadataProperty.getValue())
                .thenReturn(metadataValue);
        when(nameProperty.getName())
                .thenReturn(KEY_NAME);
        when(namespaceProperty.getName())
                .thenReturn(KEY_NAMESPACE);
        when(nameProperty.getValue())
                .thenReturn(nameValue);
        when(namespaceProperty.getValue())
                .thenReturn(namespaceValue);
        when(nameValue.getText())
                .thenReturn("\"obiwan\"");
        when(namespaceValue.getText())
                .thenReturn("\"stewjon\"");


        doAnswer(invocation -> {
            PsiElementVisitor visitor = invocation.getArgument(0);
            visitor.visitElement(metadataProperty);
            return null;
        }).when(jsonTopLevelValue).acceptChildren(any(PsiElementVisitor.class));

        doAnswer(invocation -> {
            PsiElementVisitor visitor = invocation.getArgument(0);
            visitor.visitElement(nameProperty);
            visitor.visitElement(namespaceProperty);
            return null;
        }).when(metadataValue).acceptChildren(any(PsiElementVisitor.class));

        mockCreateKubernetesTypeInfo(typeInfo, jsonFile);

        // when
        KubernetesResourceInfo resourceInfo = KubernetesResourceInfo.create(jsonFile);

        // then
        assertThat(resourceInfo).isNotNull();
        assertThat(resourceInfo.getName()).isEqualTo("\"obiwan\"");
        assertThat(resourceInfo.getNamespace()).isEqualTo("\"stewjon\"");
    }

    @Test
    public void create_returns_info_for_first_array_entry_in_multiresource_json_file() {
        // given
        JsonArray jsonTopLevelValue = mock(JsonArray.class);
        JsonValue arrayEntry = mock(JsonValue.class);
        JsonProperty metadataProperty = mock(JsonProperty.class);
        JsonValue metadataValue = mock(JsonValue.class);
        JsonProperty nameProperty = mock(JsonProperty.class);
        JsonProperty namespaceProperty = mock(JsonProperty.class);
        JsonValue nameValue = mock(JsonValue.class);
        JsonValue namespaceValue = mock(JsonValue.class);

        when(jsonFile.getTopLevelValue())
                .thenReturn(jsonTopLevelValue);
        when(jsonTopLevelValue.getChildren())
                .thenReturn(Collections
                        .singletonList(arrayEntry)
                        .toArray(new PsiElement[]{}));
        when(metadataProperty.getName())
                .thenReturn(KEY_METADATA);
        when(metadataProperty.getValue())
                .thenReturn(metadataValue);
        when(nameProperty.getName())
                .thenReturn(KEY_NAME);
        when(namespaceProperty.getName())
                .thenReturn(KEY_NAMESPACE);
        when(nameProperty.getValue())
                .thenReturn(nameValue);
        when(namespaceProperty.getValue())
                .thenReturn(namespaceValue);
        when(nameValue.getText())
                .thenReturn("\"obiwan\"");
        when(namespaceValue.getText())
                .thenReturn("\"stewjon\"");

        doAnswer(invocation -> {
            PsiElementVisitor visitor = invocation.getArgument(0);
            visitor.visitElement(metadataProperty);
            return null;
        }).when(jsonTopLevelValue).acceptChildren(any(PsiElementVisitor.class));

        doAnswer(invocation -> {
            PsiElementVisitor visitor = invocation.getArgument(0);
            visitor.visitElement(nameProperty);
            visitor.visitElement(namespaceProperty);
            return null;
        }).when(metadataValue).acceptChildren(any(PsiElementVisitor.class));

        mockCreateKubernetesTypeInfo(typeInfo, jsonFile);

        // when
        KubernetesResourceInfo resourceInfo = KubernetesResourceInfo.create(jsonFile);

        // then
        assertThat(resourceInfo).isNotNull();
        assertThat(resourceInfo.getName()).isEqualTo("\"obiwan\"");
        assertThat(resourceInfo.getNamespace()).isEqualTo("\"stewjon\"");
    }

    @Test
    public void create_returns_info_for_yaml_file() {
        // given
        YAMLKeyValue metadataKeyValue = mock(YAMLKeyValue.class);
        YAMLValue metadataValue = mock(YAMLValue.class);
        YAMLKeyValue nameKeyValue = mock(YAMLKeyValue.class);
        YAMLKeyValue namespaceKeyValue = mock(YAMLKeyValue.class);

        when(yamlFile.getDocuments())
                .thenReturn(Collections.singletonList(yamlDocument));
        when(yamlDocument.getTopLevelValue())
                .thenReturn(yamlTopLevelValue);
        when(metadataKeyValue.getName())
                .thenReturn(KEY_METADATA);
        when(metadataKeyValue.getValue())
                .thenReturn(metadataValue);
        when(nameKeyValue.getName())
                .thenReturn(KEY_NAME);
        when(namespaceKeyValue.getName())
                .thenReturn(KEY_NAMESPACE);
        when(nameKeyValue.getValueText())
                .thenReturn("obiwan");
        when(namespaceKeyValue.getValueText())
                .thenReturn("stewjon");

        doAnswer(invocation -> {
            PsiElementVisitor visitor = invocation.getArgument(0);
            visitor.visitElement(metadataKeyValue);
            return null;
        }).when(yamlTopLevelValue).acceptChildren(any(PsiElementVisitor.class));

        doAnswer(invocation -> {
            PsiElementVisitor visitor = invocation.getArgument(0);
            visitor.visitElement(nameKeyValue);
            visitor.visitElement(namespaceKeyValue);
            return null;
        }).when(metadataValue).acceptChildren(any(PsiElementVisitor.class));

        mockCreateKubernetesTypeInfo(typeInfo, yamlFile);

        // when
        KubernetesResourceInfo resourceInfo = KubernetesResourceInfo.create(yamlFile);

        // then
        assertThat(resourceInfo).isNotNull();
        assertThat(resourceInfo.getName()).isEqualTo("obiwan");
        assertThat(resourceInfo.getNamespace()).isEqualTo("stewjon");
    }

    @Test
    public void create_returns_empty_info_for_json_file_without_top_level_value() {
        // given
        when(jsonFile.getTopLevelValue())
                .thenReturn(null);
        mockCreateKubernetesTypeInfo(typeInfo, jsonFile);

        // when
        KubernetesResourceInfo resourceInfo = KubernetesResourceInfo.create(jsonFile);

        // then
        assertThat(resourceInfo).isNotNull();
        assertThat(resourceInfo.getName()).isNull();
        assertThat(resourceInfo.getNamespace()).isNull();
    }

    @Test
    public void create_returns_empty_info_for_yaml_file_without_documents() {
        // given
        when(yamlFile.getDocuments())
                .thenReturn(Collections.emptyList());
        mockCreateKubernetesTypeInfo(typeInfo, yamlFile);

        // when
        KubernetesResourceInfo resourceInfo = KubernetesResourceInfo.create(yamlFile);

        // then
        assertThat(resourceInfo).isNotNull();
        assertThat(resourceInfo.getName()).isNull();
        assertThat(resourceInfo.getNamespace()).isNull();
    }

    @Test
    public void create_returns_empty_info_for_yaml_for_file_with_null_document() {
        // given
        when(yamlFile.getDocuments())
                .thenReturn(Collections.singletonList(null));
        mockCreateKubernetesTypeInfo(typeInfo, yamlFile);

        // when
        KubernetesResourceInfo resourceInfo = KubernetesResourceInfo.create(yamlFile);

        // then
        assertThat(resourceInfo).isNotNull();
        assertThat(resourceInfo.getName()).isNull();
        assertThat(resourceInfo.getNamespace()).isNull();
    }

    @Test
    public void create_returns_empty_info_for_yaml_file_with_document_that_has_null_top_level_value() {
        // given
        when(yamlFile.getDocuments())
                .thenReturn(Collections.singletonList(yamlDocument));
        when(yamlDocument.getTopLevelValue())
                .thenReturn(null);
        mockCreateKubernetesTypeInfo(typeInfo, yamlFile);

        // when
        KubernetesResourceInfo resourceInfo = KubernetesResourceInfo.create(yamlFile);

        // then
        assertThat(resourceInfo).isNotNull();
        assertThat(resourceInfo.getName()).isNull();
        assertThat(resourceInfo.getNamespace()).isNull();
    }

    @Test
    public void equals_and_hashcode_are_correct() {
        // given
        KubernetesResourceInfo info1 = new KubernetesResourceInfo("obiwan", "stewjon", typeInfo);
        KubernetesResourceInfo info2 = new KubernetesResourceInfo("obiwan", "stewjon", typeInfo);
        KubernetesResourceInfo info3 = new KubernetesResourceInfo("luke", "stewjon", typeInfo);
        KubernetesResourceInfo info4 = new KubernetesResourceInfo("obiwan", "tatooine", typeInfo);
        KubernetesResourceInfo info5 = new KubernetesResourceInfo("obiwan", "stewjon", mock(KubernetesTypeInfo.class));

        // when & then
        assertThat(info1).isEqualTo(info1);  // same instance
        assertThat(info1).isEqualTo(info2);  // equal objects
        assertThat(info1).isNotEqualTo(null);  // null comparison
        assertThat(info1).isNotEqualTo("not a ResourceInfo");  // different class
        assertThat(info1).isNotEqualTo(info3);  // different name
        assertThat(info1).isNotEqualTo(info4);  // different namespace
        assertThat(info1).isNotEqualTo(info5);  // different typeInfo

        // Hash code
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode());
        assertThat(info1.hashCode()).isNotEqualTo(info3.hashCode());
    }

    @Test
    public void toString_concatenates_name_and_namespace() {
        // given
        KubernetesResourceInfo info = new KubernetesResourceInfo("obiwan", "stewjon", typeInfo);

        // when
        String result = info.toString();

        // then
        assertThat(result).isEqualTo("obiwan, stewjon");
    }

    @Test
    public void create_returns_empty_info_for_unsupported_file_type() {
        // given
        PsiFile unsupportedFile = mock(PsiFile.class);
        mockCreateKubernetesTypeInfo(typeInfo, unsupportedFile);

        // when
        KubernetesResourceInfo resourceInfo = KubernetesResourceInfo.create(unsupportedFile);

        // then
        assertThat(resourceInfo).isNotNull();
        assertThat(resourceInfo.getName()).isNull();
        assertThat(resourceInfo.getNamespace()).isNull();
    }

    @Test
    public void getApiGroup_returns_apiGroup_of_typeInfo() {
        // given
        KubernetesResourceInfo info = new KubernetesResourceInfo("obiwan", "stewjon", typeInfo);

        // when
        info.getApiGroup();

        // then
        verify(typeInfo).getApiGroup();
    }

    @Test
    public void getApiGroup_returns_null_given_typeInfo_is_null() {
        // given
        KubernetesResourceInfo info = new KubernetesResourceInfo("obiwan", "tatooine", null);

        // when
        String apiGroup = info.getApiGroup();

        // then
        assertThat(apiGroup).isNull();
    }

    @Test
    public void getKind_returns_kind_of_typeInfo() {
        // given
        KubernetesResourceInfo info = new KubernetesResourceInfo("luke", "stewjon", typeInfo);

        // when
        info.getKind();

        // then
        verify(typeInfo).getKind();
    }

    @Test
    public void getKind_returns_null_given_typeInfo_is_nul() {
        // given
        KubernetesResourceInfo info = new KubernetesResourceInfo("luke", "stewjon", null);

        // when
        String kind = info.getKind();

        // then
        assertThat(kind).isNull();
    }

    private void mockCreateKubernetesTypeInfo(KubernetesTypeInfo typeInfo, PsiFile file) {
        mockStatic.when(() -> KubernetesTypeInfo.create(file))
                .thenReturn(typeInfo);
    }
}