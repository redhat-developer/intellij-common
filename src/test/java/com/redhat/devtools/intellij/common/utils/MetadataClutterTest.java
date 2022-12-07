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
import com.redhat.devtools.intellij.common.BaseTest;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class MetadataClutterTest extends BaseTest {

    private static final String RESOURCE_PATH = "utils/virtualFileHelper/";

    @Test
    public void remove_ContentIsEmpty_OriginalContent() {
        String result = MetadataClutter.remove("");
        assertEquals("", result);
    }

    @Test
    public void remove_ContentHasNoMetadata_OriginalContent() throws IOException {
        String content = load(RESOURCE_PATH + "pipeline_without_metadata.yaml");
        String result = MetadataClutter.remove(content);
        assertEquals(content, result);
    }

    @Test
    public void remove_ContentHasMetadataWithoutClutterTags_OriginalContent() throws IOException {
        String content = load(RESOURCE_PATH + "pipeline_with_no_clutters.yaml");
        JsonNode content_Node = YAMLHelper.YAMLToJsonNode(content);
        String result = MetadataClutter.remove(content);
        JsonNode result_Node = YAMLHelper.YAMLToJsonNode(result);
        assertEquals(content_Node.get("metadata"), result_Node.get("metadata"));
        assertEquals(content_Node.get("spec"), result_Node.get("spec"));
    }

    @Test
    public void remove_ContentHasMetadataWithoutClutterTags_CleanedContent() throws IOException {
        String content = load(RESOURCE_PATH + "pipeline_with_clutters.yaml");
        String content_without_clutters = load(RESOURCE_PATH + "pipeline_with_no_clutters.yaml");
        JsonNode content_without_clutters_Node = YAMLHelper.YAMLToJsonNode(content_without_clutters);
        String result = MetadataClutter.remove(content, false);
        JsonNode result_Node = YAMLHelper.YAMLToJsonNode(result);
        assertEquals(content_without_clutters_Node.get("metadata"), result_Node.get("metadata"));
        assertEquals(content_without_clutters_Node.get("spec"), result_Node.get("spec"));
    }

    @Test
    public void remove_should_return_null_if_metadata_is_null() {
        // given
        // when
        ObjectMeta mangled = MetadataClutter.remove((ObjectMeta) null);
        // then
        assertThat(mangled).isNull();
    }

    @Test
    public void remove_should_return_unchanged_metadata_if_has_no_clutter() {
        // given
        ObjectMeta metadata = new ObjectMetaBuilder()
                .withName("john wayne")
                .withNamespace("wild west")
                .withLabels(Collections.singletonMap("hat", "cowboy"))
                .build();
        // when
        ObjectMeta mangled = MetadataClutter.remove(metadata);
        // then
        assertThat(mangled.getName()).isEqualTo("john wayne");
        assertThat(mangled.getNamespace()).isEqualTo("wild west");
        assertThat(mangled.getLabels()).containsAllEntriesOf(Collections.singletonMap("hat", "cowboy"));
    }

    @Test
    public void remove_should_remove_all_clutters() {
        // given
        ObjectMeta metadata = new ObjectMetaBuilder()
                .withName("john wayne")
                .withNamespace("wild west")
                .withLabels(Collections.singletonMap("hat", "cowboy"))
                // clutter
                .withCreationTimestamp("20th century")
                .withDeletionGracePeriodSeconds(42L)
                .withDeletionTimestamp("June 11, 1979")
                .withFinalizers("Comancheros", "Sons of Katie Elder")
                .withGeneration(42L)
                .withManagedFields(
                        new ManagedFieldsEntry("rifle", "winchester", null, null, null, null, null)
                )
                .withOwnerReferences(new OwnerReference("gun", true, true, "colt", null, null))
                .withResourceVersion("42")
                .withSelfLink("not too serious")
                .withUid("42")
                .withAdditionalProperties(
                        Map.of("clusterName", "Wild West")
                )
                .build();
        // when
        ObjectMeta mangled = MetadataClutter.remove(metadata);
        // then
        assertThat(mangled.getCreationTimestamp()).isNull();
        assertThat(mangled.getDeletionGracePeriodSeconds()).isNull();
        assertThat(mangled.getDeletionTimestamp()).isNull();
        assertThat(mangled.getFinalizers()).isEmpty();
        assertThat(mangled.getGeneration()).isNull();
        assertThat(mangled.getManagedFields()).isEmpty();
        assertThat(mangled.getOwnerReferences()).isEmpty();
        assertThat(mangled.getResourceVersion()).isNull();
        assertThat(mangled.getSelfLink()).isNull();
        assertThat(mangled.getUid()).isNull();
        assertThat(mangled.getAdditionalProperties().get("clusterName")).isNull();
    }

    @Test
    public void remove_should_remove_same_clutter_as_remove_from_text () throws IOException {
        // given
        String withClutter = load(RESOURCE_PATH + "pipeline_with_clutters.yaml");
        String noClutter = MetadataClutter.remove(withClutter);

        GenericKubernetesResource resourceWithClutter = Serialization.unmarshal(withClutter, GenericKubernetesResource.class);
        ObjectMeta metaWithClutter = new ObjectMetaBuilder(resourceWithClutter.getMetadata()).build();

        // when
        ObjectMeta metaWithoutClutter = MetadataClutter.remove(metaWithClutter);
        // then
        GenericKubernetesResource noClutterResource = Serialization.unmarshal(noClutter);
        assertThat(metaWithoutClutter).isEqualTo(noClutterResource.getMetadata());
    }

    @Test
    public void remove_ContentHasMetadataWithoutClutterTagsAndMinimizeQuotes_CleanedContent() throws IOException {
        String content = load(RESOURCE_PATH + "pipeline_with_clutters_2.yaml");
        String content_without_clutters = load(RESOURCE_PATH + "pipeline_without_clutters_2_minimize_quotes.yaml");
        JsonNode content_without_clutters_Node = YAMLHelper.YAMLToJsonNode(content_without_clutters);
        String result = MetadataClutter.remove(content, true);
        JsonNode result_Node = YAMLHelper.YAMLToJsonNode(result);
        assertEquals(content_without_clutters_Node.get("metadata"), result_Node.get("metadata"));
        assertEquals(content_without_clutters_Node.get("spec"), result_Node.get("spec"));
    }

    @Test
    public void remove_ContentHasMetadataWithoutClutterTagsAndQuotes_CleanedContent() throws IOException {
        String content = load(RESOURCE_PATH + "pipeline_with_clutters_2.yaml");
        String content_without_clutters = load(RESOURCE_PATH + "pipeline_without_clutters_2_with_quotes.yaml");
        JsonNode content_without_clutters_Node = YAMLHelper.YAMLToJsonNode(content_without_clutters);
        String result = MetadataClutter.remove(content, false);
        JsonNode result_Node = YAMLHelper.YAMLToJsonNode(result);
        assertEquals(content_without_clutters_Node.get("metadata"), result_Node.get("metadata"));
        assertEquals(content_without_clutters_Node.get("spec"), result_Node.get("spec"));
    }

}
