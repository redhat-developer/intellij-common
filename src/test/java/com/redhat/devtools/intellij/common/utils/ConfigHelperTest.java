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

import io.fabric8.kubernetes.api.model.Context;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.Config;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ConfigHelperTest {

    private static final NamedContext ctx1 = new NamedContext(
            new Context("cluster1",
                    null,
                    "namespace1",
                    "papa smurf"),
            "papa smurfs context");
    private static final NamedContext ctx2 = new NamedContext(
            new Context("cluster2",
                    null,
                    "namespace2",
                    "grumpy smurf"),
            "grumpy smurfs context");
    private static final NamedContext ctx3 = new NamedContext(
            new Context("cluster3",
                    null,
                    "namespace3",
                    "smurfette"),
            "smurfettes context");
    private static final NamedContext ctx4 = new NamedContext(
            new Context("cluster4",
                    null,
                    "namespace4",
                    "jokey smurf"),
            "jokey smurfs context");
    private static final NamedContext ctx5 = new NamedContext(
            new Context("cluster2",
                    null,
                    "namespace2",
                    "azrael"),
            "azraels context");

    private static final List<NamedContext> allContextsButCtx4 = Arrays.asList(ctx1, ctx2, ctx3, ctx5); // ctx4 not included

    @Test
    public void areEqualCurrentContext_returns_true_given_identical_current_contexts() {
        // given
        Config config1 = clientConfig(ctx1);
        Config config2 = clientConfig(ctx1);
        // when
        boolean equal = ConfigHelper.areEqualCurrentContext(config1, config2);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void areEqualCurrentContext_returns_false_given_one_config_has_no_current_context_and_the_other_has() {
        // given
        Config config1 = clientConfig((NamedContext) null);
        Config config2 = clientConfig(ctx1);
        // when
        boolean equal = ConfigHelper.areEqualCurrentContext(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqualCurrentContext_returns_false_given_current_contexts_differ_in_cluster() {
        // given
        Config config1 = clientConfig(ctx1);
        NamedContext differentCluster = clone(ctx1);
        differentCluster.getContext().setCluster("imperial fleet");
        Config config2 = clientConfig(differentCluster);
        // when
        boolean equal = ConfigHelper.areEqualCurrentContext(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqualCurrentContext_returns_false_given_current_contexts_differ_in_namespace() {
        // given
        Config config1 = clientConfig(ctx1);
        NamedContext differentNamespace = clone(ctx1);
        differentNamespace.getContext().setNamespace("stormtroopers");
        Config config2 = clientConfig(differentNamespace);
        // when
        boolean equal = ConfigHelper.areEqualCurrentContext(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqualCurrentContext_returns_false_given_current_contexts_differ_in_user() {
        // given
        Config config1 = clientConfig(ctx1);
        NamedContext differentUser = clone(ctx1);
        differentUser.getContext().setUser("lord vader");
        Config config2 = clientConfig(differentUser);
        // when
        boolean equal = ConfigHelper.areEqualCurrentContext(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqualCurrentContext_returns_false_given_current_contexts_differ_in_name() {
        // given
        Config config1 = clientConfig(ctx1);
        NamedContext differentName = clone(ctx1);
        differentName.setName("r2d2");
        Config config2 = clientConfig(differentName);
        // when
        boolean equal = ConfigHelper.areEqualCurrentContext(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqualContexts_returns_true_given_contexts_with_same_members() {
        // given
        Config config1 = clientConfig(allContextsButCtx4);
        List<NamedContext> sameMembers = clone(allContextsButCtx4);
        Config config2 = clientConfig(sameMembers);
        // when
        boolean equal = ConfigHelper.areEqualContexts(config1, config2);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void areEqualContexts_returns_false_given_config_is_missing_one_context() {
        // given
        Config config1 = clientConfig(allContextsButCtx4);
        List<NamedContext> isMissingOneContext = clone(allContextsButCtx4);
        isMissingOneContext.remove(isMissingOneContext.size() - 1);
        Config config2 = clientConfig(isMissingOneContext);
        // when
        boolean equal = ConfigHelper.areEqualContexts(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqualContexts_returns_false_given_config_has_additional_context() {
        // given
        Config config1 = clientConfig(allContextsButCtx4);
        List<NamedContext> hasAdditionalCtx4 = clone(allContextsButCtx4);
        hasAdditionalCtx4.add(ctx4);
        Config config2 = clientConfig(hasAdditionalCtx4);
        // when
        boolean equal = ConfigHelper.areEqualContexts(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqualAuthInfo_returns_false_given_contexts_differ_in_username() {
        // given
        Config config1 = clientConfig("yoda", null);
        Config config2 = clientConfig("obiwan", null);
        // when
        boolean equal = ConfigHelper.areEqualAuthInfo(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqualAuthInfo_returns_false_given_contexts_differ_in_password() {
        // given
        Config config1 = clientConfig("yoda", "the force");
        Config config2 = clientConfig("yoda", "the light saber");
        // when
        boolean equal = ConfigHelper.areEqualAuthInfo(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqualToken_returns_true_given_contexts_have_same_token() {
        // given
        Config config1 = clientConfig("R2-D2");
        Config config2 = clientConfig("R2-D2");
        // when
        boolean equal = ConfigHelper.areEqualToken(config1, config2);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void areEqualToken_returns_false_given_contexts_differ_in_token() {
        // given
        Config config1 = clientConfig("R2-D2");
        Config config2 = clientConfig("C-3PO");
        // when
        boolean equal = ConfigHelper.areEqualToken(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqual_returns_true_given_both_context_have_same_token_current_ctx_and_contexts() {
        // given
        Config config1 = clientConfig("C3-PO", ctx2, allContextsButCtx4);
        Config config2 = clientConfig("C3-PO", clone(ctx2), clone(allContextsButCtx4));
        // when
        boolean equal = ConfigHelper.areEqual(config1, config2);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void areEqual_returns_false_given_one_context_has_different_token() {
        // given
        Config config1 = clientConfig("C3-PO", ctx2, allContextsButCtx4);
        Config config2 = clientConfig("R2-D2", clone(ctx2), clone(allContextsButCtx4));
        // when
        boolean equal = ConfigHelper.areEqual(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqual_returns_false_given_one_context_has_different_current_context() {
        // given
        Config config1 = clientConfig("C3-PO", ctx2, allContextsButCtx4);
        Config config2 = clientConfig("C3-PO", ctx3, clone(allContextsButCtx4));
        // when
        boolean equal = ConfigHelper.areEqual(config1, config2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void areEqual_returns_true_even_if_one_context_has_additional_context() {
        // given
        Config config1 = clientConfig("C3-PO", ctx2, allContextsButCtx4);
        List<NamedContext> hasAdditionalContext = clone(allContextsButCtx4);
        hasAdditionalContext.add(ctx4);
        Config config2 = clientConfig("C3-PO", clone(ctx2), hasAdditionalContext);
        // when
        boolean equal = ConfigHelper.areEqual(config1, config2);
        // then different number of members doesn't matter, only current context matters
        assertThat(equal).isTrue();
    }

    private static Config clientConfig(String token) {
        return clientConfig(token, null, null);
    }

    private static Config clientConfig(String username, String password) {
        Config config = clientConfig(null, null, null);
        doReturn(username)
                .when(config).getUsername();
        doReturn(password)
                .when(config).getPassword();
        return config;
    }

    private static Config clientConfig(NamedContext currentContext) {
        return clientConfig(null, currentContext, null);
    }

    private static Config clientConfig(List<NamedContext> contexts) {
        return clientConfig(null, null, contexts);
    }

    private static Config clientConfig(String token, NamedContext currentContext, List<NamedContext> contexts) {
        Config config = mock(Config.class);
        doReturn(token)
                .when(config).getAutoOAuthToken();
        doReturn(currentContext)
                .when(config).getCurrentContext();
        doReturn(contexts)
                .when(config).getContexts();
        return config;
    }

    private static List<NamedContext> clone(List<NamedContext> namedContexts) {
        return namedContexts.stream()
                .map(ConfigHelperTest::clone)
                .collect(Collectors.toList());
    }

    private static NamedContext clone(NamedContext namedContext) {
        Context context = namedContext.getContext();
        return new NamedContext(
                new Context(context.getCluster(),
                        context.getExtensions(),
                        context.getNamespace(),
                        context.getUser()),
                namedContext.getName());
    }
}
