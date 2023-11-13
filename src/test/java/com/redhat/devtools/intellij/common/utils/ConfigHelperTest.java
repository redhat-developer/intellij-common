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

import io.fabric8.kubernetes.api.model.AuthInfo;
import io.fabric8.kubernetes.api.model.AuthInfoBuilder;
import io.fabric8.kubernetes.api.model.AuthProviderConfig;
import io.fabric8.kubernetes.api.model.Context;
import io.fabric8.kubernetes.api.model.NamedAuthInfo;
import io.fabric8.kubernetes.api.model.NamedAuthInfoBuilder;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.Config;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final NamedAuthInfo user1 = new NamedAuthInfo(
            ctx1.getContext().getUser(),
            authInfo(null, authProviderConfig("token1")));
    private static final NamedContext ctx2 = new NamedContext(
            new Context("cluster2",
                    null,
                    "namespace2",
                    "grumpy smurf"),
            "grumpy smurfs context");
    private static final NamedAuthInfo user2 = new NamedAuthInfo(
            ctx2.getContext().getUser(),
            authInfo(null, authProviderConfig("token2")));
    private static final NamedContext ctx3 = new NamedContext(
            new Context("cluster3",
                    null,
                    "namespace3",
                    "smurfette"),
            "smurfettes context");
    private static final NamedAuthInfo user3 = new NamedAuthInfo(
            ctx3.getContext().getUser(),
            authInfo(null, authProviderConfig("token3")));
    private static final NamedContext ctx4 = new NamedContext(
            new Context("cluster4",
                    null,
                    "namespace4",
                    "jokey smurf"),
            "jokey smurfs context");
    private static final NamedAuthInfo user4 = new NamedAuthInfo(
            ctx4.getContext().getUser(),
            authInfo(null, authProviderConfig("token4")));
    private static final NamedContext ctx5 = new NamedContext(
            new Context("cluster2",
                    null,
                    "namespace2",
                    "azrael"),
            "azraels context");
    private static final NamedAuthInfo user5 = new NamedAuthInfo(
            ctx5.getContext().getUser(),
            authInfo("token1", null));


    private static final List<NamedAuthInfo> allUsers = Arrays.asList(user1, user2, user3, user4, user5);
    private static final List<NamedContext> allContexts = Arrays.asList(ctx1, ctx2, ctx3, ctx5); // ctx4 not included

    @Test
    public void identical_namedContexts_should_be_equal() {
        // given
        // when
        boolean equal = ConfigHelper.areEqual(ctx1, clone(ctx1));
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void null_namedContexts_should_not_be_equal_to_non_null_namedContext() {
        // given
        // when
        boolean equal = ConfigHelper.areEqual(ctx1, null);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void non_null_namedContexts_should_not_be_equal_to_null_namedContext() {
        // given
        // when
        boolean equal = ConfigHelper.areEqual(null, ctx1);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void namedContexts_with_different_cluster_should_NOT_be_equal() {
        // given
        NamedContext differentCluster = clone(ctx1);
        differentCluster.getContext().setCluster("imperial fleet");
        // when
        boolean equal = ConfigHelper.areEqual(ctx1, differentCluster);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void namedContexts_with_different_namespace_should_NOT_be_equal() {
        // given
        NamedContext differentNamespace = clone(ctx1);
        differentNamespace.getContext().setNamespace("stormtroopers");
        // when
        boolean equal = ConfigHelper.areEqual(ctx1, differentNamespace);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void namedContexts_with_different_user_should_NOT_be_equal() {
        // given
        NamedContext differentUser = clone(ctx1);
        differentUser.getContext().setUser("lord vader");
        // when
        boolean equal = ConfigHelper.areEqual(ctx1, differentUser);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void namedContexts_with_different_name_should_NOT_be_equal() {
        // given
        NamedContext differentName = clone(ctx1);
        differentName.setName("imperial fleet");
        // when
        boolean equal = ConfigHelper.areEqual(ctx1, differentName);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void namedContexts_with_same_members_should_be_equal() {
        // given
        // when
        boolean equal = ConfigHelper.areEqual(
                Arrays.asList(ctx1, ctx2),
                Arrays.asList(clone(ctx1), clone(ctx2)));
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void namedContexts_with_additional_member_should_NOT_be_equal() {
        // given

        // when
        boolean equal = ConfigHelper.areEqual(
                Arrays.asList(ctx1, ctx2),
                Arrays.asList(clone(ctx1), clone(ctx2), ctx3));
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void namedContexts_with_different_members_should_NOT_be_equal() {
        // given
        List<NamedContext> additionalMember = clone(allContexts);
        additionalMember.add(ctx4);
        // when
        boolean equal = ConfigHelper.areEqual(
                allContexts,
                additionalMember);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void authInfo_with_same_provider_token_should_be_equal() {
        // given
        String token = "gargamel";
        AuthInfo authInfo = authInfo(null, authProviderConfig("id-token", token));
        Config config = clientConfig(token);
        // when
        boolean equal = ConfigHelper.areEqualToken(authInfo, config);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void authInfo_with_same_authinfo_token_should_be_equal() {
        // given
        String token = "token42";
        AuthInfo authInfo = authInfo(token, null);
        Config config = clientConfig(token);
        // when
        boolean equal = ConfigHelper.areEqualToken(authInfo, config);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void authInfo_with_different_provider_token_should_NOT_be_equal() {
        // given
        AuthInfo authInfo = authInfo(null, authProviderConfig("id-token", "gargamel"));
        Config config = clientConfig("azrael");
        // when
        boolean equal = ConfigHelper.areEqualToken(authInfo, config);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void authInfo_with_same_provider_token_in_access_token_should_be_equal() {
        // given
        String token = "gargamel";
        AuthInfo authInfo = authInfo(null, authProviderConfig("access-token", token));
        Config config = clientConfig(token);
        // when
        boolean equal = ConfigHelper.areEqualToken(authInfo, config);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void two_authInfos_with_same_provider_token_should_be_equal() {
        // given
        String token = "gargamel";
        AuthInfo authInfo = authInfo(null, authProviderConfig("id-token", token));
        AuthInfo newAuthInfo = authInfo(null, authProviderConfig("id-token", token));
        // when
        boolean equal = ConfigHelper.areEqualToken(authInfo, newAuthInfo);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void two_authInfos_with_same_authinfo_token_should_be_equal() {
        // given
        String token = "token42";
        AuthInfo authInfo = authInfo(token, null);
        AuthInfo newAuthInfo = authInfo(token, null);
        // when
        boolean equal = ConfigHelper.areEqualToken(authInfo, newAuthInfo);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void two_authInfos_with_different_provider_token_should_NOT_be_equal() {
        // given
        AuthInfo authInfo = authInfo(null, authProviderConfig("id-token", "gargamel"));
        AuthInfo newAuthInfo = authInfo(null, authProviderConfig("id-token", "azrael"));
        // when
        boolean equal = ConfigHelper.areEqualToken(authInfo, newAuthInfo);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void two_authInfos_with_same_provider_token_in_access_token_should_be_equal() {
        // given
        String token = "gargamel";
        AuthInfo authInfo = authInfo(null, authProviderConfig("access-token", token));
        AuthInfo newAuthInfo = authInfo(null, authProviderConfig("access-token", token));
        // when
        boolean equal = ConfigHelper.areEqualToken(authInfo, newAuthInfo);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void kubeConfig1_and_kubeConfig2_are_equal_if_same_in_currentContext_contexts_and_provider_token() {
        // given
        io.fabric8.kubernetes.api.model.Config kubeConfig1 = kubeConfig(
                ctx2,
                allContexts,
                allUsers);
        io.fabric8.kubernetes.api.model.Config kubeConfig2 = kubeConfig(
                clone(ctx2),
                clone(allContexts),
                clone(allUsers));
        // when
        boolean equal = ConfigHelper.areEqual(kubeConfig1, kubeConfig2);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void kubeConfig1_and_kubeConfig2_are_NOT_equal_if_NOT_same_in_currentContext() {
        // given
        io.fabric8.kubernetes.api.model.Config kubeConfig1 = kubeConfig(
                ctx2,
                allContexts,
                allUsers);
        io.fabric8.kubernetes.api.model.Config kubeConfig2 = kubeConfig(
                clone(ctx3),
                clone(allContexts),
                clone(allUsers));
        // when
        boolean equal = ConfigHelper.areEqual(kubeConfig1, kubeConfig2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void kubeConfig1_and_kubeConfig2_are_NOT_equal_if_contexts_has_additional_member() {
        // given
        List<NamedContext> allContextsWithAddition = new ArrayList<>(allContexts);
        allContextsWithAddition.add(ctx4);
        io.fabric8.kubernetes.api.model.Config kubeConfig1 = kubeConfig(
                ctx2,
                allContextsWithAddition,
                allUsers);
        io.fabric8.kubernetes.api.model.Config kubeConfig2 = kubeConfig(
                clone(ctx2),
                clone(allContexts),
                clone(allUsers));
        // when
        boolean equal = ConfigHelper.areEqual(kubeConfig1, kubeConfig2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void kubeConfig1_and_kubeConfig2_are_NOT_equal_if_token_is_different() {
        // given
        io.fabric8.kubernetes.api.model.Config kubeConfig1 = kubeConfig(
                ctx2,
                allContexts,
                allUsers);

        String currentUserName = ctx2.getContext().getUser();
        NamedAuthInfo currentUser = allUsers.stream()
                .filter(user -> user.getName().equals(currentUserName))
                .findFirst()
                .get();
        NamedAuthInfo currentUserClone = clone(currentUser);
        List<NamedAuthInfo> allUsersClone = clone(allUsers);
        int index = allUsersClone.indexOf(currentUserClone);
        allUsersClone.set(index, currentUserClone);
        currentUserClone.getUser().setToken("token 42");
        io.fabric8.kubernetes.api.model.Config kubeConfig2 = kubeConfig(
                clone(ctx2),
                clone(allContexts),
                allUsersClone);
        // when
        boolean equal = ConfigHelper.areEqual(kubeConfig1, kubeConfig2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void kubeConfig1_and_kubeConfig2_are_NOT_equal_if_kubeConfig2_has_no_current_context() {
        // given
        io.fabric8.kubernetes.api.model.Config kubeConfig1 = kubeConfig(
                ctx2,
                allContexts,
                allUsers);
        io.fabric8.kubernetes.api.model.Config kubeConfig2 = kubeConfig(
                null, // no current context
                allContexts,
                allUsers);
        // when
        boolean equal = ConfigHelper.areEqual(kubeConfig1, kubeConfig2);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void kubeConfig_and_clientConfig_are_equal_if_same_in_currentContext_contexts_and_provider_token() {
        // given
        io.fabric8.kubernetes.api.model.Config kubeConfig = kubeConfig(
                ctx2,
                allContexts,
                allUsers);
        io.fabric8.kubernetes.client.Config clientConfig = clientConfig(
                getUser(ctx2, allUsers),
                clone(ctx2),
                clone(allContexts));
        // when
        boolean equal = ConfigHelper.areEqual(kubeConfig, clientConfig);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void kubeConfig_and_clientConfig_are_equal_if_same_in_currentContext_contexts_and_authinfo_token() {
        // given
        io.fabric8.kubernetes.api.model.Config kubeConfig = kubeConfig(
                ctx5,
                allContexts,
                allUsers);
        io.fabric8.kubernetes.client.Config clientConfig = clientConfig(
                getUser(ctx5, allUsers),
                clone(ctx5),
                clone(allContexts));
        // when
        boolean equal = ConfigHelper.areEqual(kubeConfig, clientConfig);
        // then
        assertThat(equal).isTrue();
    }

    @Test
    public void kubeConfig_and_clientConfig_are_NOT_equal_if_NOT_same_in_currentContext() {
        // given
        io.fabric8.kubernetes.api.model.Config kubeConfig = kubeConfig(
                ctx2,
                allContexts,
                allUsers);
        io.fabric8.kubernetes.client.Config clientConfig = clientConfig(
                getUser(ctx3, allUsers),
                clone(ctx3),
                clone(allContexts));
        // when
        boolean equal = ConfigHelper.areEqual(kubeConfig, clientConfig);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void kubeConfig_and_clientConfig_are_NOT_equal_if_contexts_has_additional_member() {
        // given
        List<NamedContext> allContextsWithAddition = new ArrayList<>(allContexts);
        allContextsWithAddition.add(ctx4);
        io.fabric8.kubernetes.api.model.Config kubeConfig = kubeConfig(
                ctx2,
                allContextsWithAddition,
                allUsers);
        io.fabric8.kubernetes.client.Config clientConfig = clientConfig(
                getUser(ctx2, allUsers),
                clone(ctx2),
                allContexts);
        // when
        boolean equal = ConfigHelper.areEqual(kubeConfig, clientConfig);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void kubeConfig_and_clientConfig_are_NOT_equal_if_token_is_different() {
        // given
        io.fabric8.kubernetes.api.model.Config kubeConfig = kubeConfig(
                ctx2,
                allContexts,
                allUsers);
        io.fabric8.kubernetes.client.Config clientConfig = clientConfig(
                "token 42",
                clone(ctx2),
                allContexts);
        // when
        boolean equal = ConfigHelper.areEqual(kubeConfig, clientConfig);
        // then
        assertThat(equal).isFalse();
    }

    @Test
    public void kubeConfig_and_clientConfig_are_NOT_equal_if_kubeConfig_has_no_current_context() {
        // given
        io.fabric8.kubernetes.api.model.Config kubeConfig = kubeConfig(
                null, // no current context
                allContexts,
                allUsers);
        io.fabric8.kubernetes.client.Config clientConfig = clientConfig(
                "token 42",
                clone(ctx2),
                allContexts);
        // when
        boolean equal = ConfigHelper.areEqual(kubeConfig, clientConfig);
        // then
        assertThat(equal).isFalse();
    }

    private static AuthInfo authInfo(String token, AuthProviderConfig config) {
        AuthInfo authInfo = new AuthInfo();
        authInfo.setAuthProvider(config);
        authInfo.setToken(token);
        return authInfo;
    }

    private static AuthProviderConfig authProviderConfig(String tokenValue) {
        return authProviderConfig("id-config", tokenValue);
    }

    private static AuthProviderConfig authProviderConfig(String tokenKey, String tokenValue) {
        AuthProviderConfig authProviderConfig = new AuthProviderConfig();
        Map<String, String> config = new HashMap<>();
        config.put(tokenKey, tokenValue);
        authProviderConfig.setConfig(config);
        return authProviderConfig;
    }

    private static Config clientConfig(String token) {
        return clientConfig(token, null, Collections.emptyList());
    }

    private static Config clientConfig(NamedAuthInfo user, NamedContext currentContext, List<NamedContext> contexts) {
        return clientConfig(user.getUser().getToken(), currentContext, contexts);
    }

    private static Config clientConfig(String token, NamedContext currentContext, List<NamedContext> contexts) {
        Config config = mock(Config.class);
        doReturn(token)
                .when(config).getOauthToken();
        doReturn(currentContext)
                .when(config).getCurrentContext();
        doReturn(contexts)
                .when(config).getContexts();
        return config;
    }

    private static io.fabric8.kubernetes.api.model.Config kubeConfig(
            NamedContext currentContext,
            List<NamedContext> contexts,
            List<NamedAuthInfo> users) {
        io.fabric8.kubernetes.api.model.Config config = mock(io.fabric8.kubernetes.api.model.Config.class);
        doReturn(currentContext == null? null : currentContext.getName())
                .when(config).getCurrentContext();
        doReturn(contexts)
                .when(config).getContexts();
        doReturn(users)
                .when(config).getUsers();
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

    private static NamedAuthInfo clone(NamedAuthInfo user) {
        return new NamedAuthInfoBuilder(user).build();
    }

    private static List<NamedAuthInfo> clone(Collection<NamedAuthInfo> users) {
        return users.stream()
                .map(user -> new NamedAuthInfoBuilder(user).build())
                .collect(Collectors.toList());
    }

    private static NamedAuthInfo getUser(NamedContext context, Collection<NamedAuthInfo> users) {
        return users.stream()
                .filter(user -> user.getName() == context.getContext().getUser())
                .findAny()
                .orElse(null);
    }
}
