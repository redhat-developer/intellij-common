/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
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
import io.fabric8.kubernetes.api.model.AuthInfo;
import io.fabric8.kubernetes.api.model.AuthProviderConfig;
import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.model.ConfigBuilder;
import io.fabric8.kubernetes.api.model.Context;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class ConfigHelper {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public static String getKubeConfigPath() {
        return io.fabric8.kubernetes.client.Config.getKubeconfigFilename();
    }

    public static void saveKubeConfig(Config config) throws IOException {
        mapper.writeValue(new File(getKubeConfigPath()), config);
    }

    public static Config safeLoadKubeConfig() {
        try {
            return loadKubeConfig();
        } catch (IOException e) {
            return null;
        }
    }

    public static Config loadKubeConfig() throws IOException {
        return loadKubeConfig(getKubeConfigPath());
    }

    public static Config loadKubeConfig(String path) throws IOException {
        File f = new File(path);
        if (f.exists()) {
            return KubeConfigUtils.parseConfig(f);
        } else {
            return new ConfigBuilder().build();
        }
    }

    public static boolean isKubeConfigParsable() {
        return isKubeConfigParsable(new File(getKubeConfigPath()));
    }

    public static boolean isKubeConfigParsable(File kubeConfig) {
        try {
            mapper.readValue(kubeConfig, Config.class);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static ToolsConfig loadToolsConfig() throws IOException {
        return loadToolsConfig(ConfigHelper.class.getResource("/tools.json"));
    }

    public static ToolsConfig loadToolsConfig(URL url) throws IOException {
        return mapper.readValue(url, ToolsConfig.class);
    }

    public static NamedContext getCurrentContext() {
        try {
            Config config = loadKubeConfig();
            return KubeConfigUtils.getCurrentContext(config);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns {@code true} if the given {@link io.fabric8.kubernetes.api.model.Config} and
     * the new {@link io.fabric8.kubernetes.api.model.Config} are equal. They are considered equal if they're
     * equal in
     * <ul>
     *     <li>current context (cluster, user, current namespace, extensions)</li>
     *     <li>(authentication) token</li>
     * </ul>
     *
     * @param kubeConfig the (file) config to compare
     * @param newKubeConfig the (client, runtime) config to compare
     * @return true if both configs are equal in context, contexts and token
     */
    public static boolean areEqualCurrentContext(Config kubeConfig, Config newKubeConfig) {
        if (newKubeConfig == null) {
            return kubeConfig == null;
        } else if (kubeConfig == null) {
            return false;
        }
        return areEqual(KubeConfigUtils.getCurrentContext(newKubeConfig), KubeConfigUtils.getCurrentContext(kubeConfig))
                && areEqualToken(kubeConfig, newKubeConfig);
    }

    /**
     * Returns {@code true} if the given {@link io.fabric8.kubernetes.api.model.Config} and
     * (client runtime) {@link io.fabric8.kubernetes.client.Config} are equal. They are considered equal if they're
     * equal in
     * <ul>
     *     <li>current context (cluster, user, current namespace, extensions)</li>
     *     <li>(existing) contexts</li>
     *     <li>(authentication) token</li>
     * </ul>
     *
     * @param kubeConfig the (file) config to compare
     * @param clientConfig the (client, runtime) config to compare
     * @return true if both configs are equal in context, contexts and token
     */
    public static boolean areEqual(Config kubeConfig, io.fabric8.kubernetes.client.Config clientConfig) {
        if (clientConfig == null) {
            return kubeConfig == null;
        } else if (kubeConfig == null) {
            return false;
        }
        return areEqual(clientConfig.getCurrentContext(), KubeConfigUtils.getCurrentContext(kubeConfig))
                && areEqual(clientConfig.getContexts(), kubeConfig.getContexts())
        			&& areEqualToken(kubeConfig, clientConfig);
    }

    /**
     * Returns {@code true} if both given contexts are equal. They are considered equal if they're equal in
     * <ul>
     *     <li>cluster</li>
     *     <li>user</li>
     *     <li>current namespace</li>
     *     <li>extensions</li>
     * </ul>
     *
     * @param thisContext the first context to compare
     * @param thatContext the second context to compare
     * @return true if both contexts are equal
     *
     * @see NamedContext
     * @see Context
     */
    public static boolean areEqual(NamedContext thisContext, NamedContext thatContext) {
        if (thisContext == null) {
            return thatContext == null;
        } else if (thatContext == null) {
            return false;
        }
        if (!Objects.equals(thisContext.getName(), thatContext.getName())) {
            return false;
        }

        return areEqual(thisContext.getContext(), thatContext.getContext());
    }

    private static boolean areEqual(Context thisContext, Context thatContext) {
        if (thisContext == null) {
            return thatContext == null;
        } else if (thatContext == null) {
            return false;
        }

        if (!Objects.equals(thisContext.getCluster(), thatContext.getCluster())){
            return false;
        } else if (!Objects.equals(thisContext.getNamespace(), thatContext.getNamespace())){
            return false;
        } else {
            return Objects.equals(thisContext.getUser(), thatContext.getUser());
        }
    }

    public static boolean areEqual(Collection<NamedContext> these, Collection<NamedContext> those) {
        if (these == null) {
            return those == null;
        } else if (those == null) {
            return false;
        }
        return these.size() == those.size()
                && these.stream()
                        .allMatch(namedContext -> contains(namedContext, those));
    }

    private static boolean contains(NamedContext namedContext, Collection<NamedContext> namedContexts) {
        if (namedContexts == null
                || namedContexts.isEmpty()) {
            return false;
        }
        return namedContexts.stream()
                .anyMatch(named -> areEqual(namedContext, named));
    }

    /**
     * Returns {@code true} if the token in the given (kubernetes file) {@link io.fabric8.kubernetes.api.model.Config}
     * and (client runtime) {@link io.fabric8.kubernetes.client.Config} are equal.
     * Returns {@code false} otherwise.
     *
     * @param kubeConfig the (kube config) auth info that contains the token
     * @param clientConfig the (client) config that contains the token
     * @return true if both tokens are equal, false otherwise
     */
    public static boolean areEqualToken(Config kubeConfig, io.fabric8.kubernetes.client.Config clientConfig) {
        return areEqualToken(getAuthInfo(kubeConfig), clientConfig);
    }

    /**
     * Returns {@code true} if the token in the given (kubernetes file) {@link io.fabric8.kubernetes.api.model.Config}
     * and the one in the new Kubernetes file {@link io.fabric8.kubernetes.api.model.Config} are equal.
     * Returns {@code false} otherwise.
     *
     * @param kubeConfig the (kube config) auth info that contains the token
     * @param newKubeConfig the (client) config that contains the token
     * @return true if both tokens are equal, false otherwise
     */
    public static boolean areEqualToken(Config kubeConfig, Config newKubeConfig) {
        return areEqualToken(getAuthInfo(kubeConfig), getAuthInfo(newKubeConfig));
    }

    private static AuthInfo getAuthInfo(Config kubeConfig) {
        NamedContext current = KubeConfigUtils.getCurrentContext(kubeConfig);
        if (current == null) {
            return null;
        }
        return KubeConfigUtils.getUserAuthInfo(kubeConfig, current.getContext());
    }

    /**
     * Returns {@code true} if the token in the given {@link AuthInfo} (that's retrieved from the kube config file)
     * and {@link Config} (that's contains the runtime settings that the kubernetes-client is using) are equal.
     * Returns {@code false} otherwise.
     *
     * @param authInfo the (kube config) auth info that contains the token
     * @param clientConfig the (client) config that contains the token
     * @return true if both tokens are equal, false otherwise
     */
    public static boolean areEqualToken(AuthInfo authInfo, io.fabric8.kubernetes.client.Config clientConfig) {
        String kubeConfigToken = getToken(authInfo);
        if (clientConfig == null
            || clientConfig.getOauthToken() == null) {
            return kubeConfigToken == null;
        }
        return clientConfig.getOauthToken().equals(kubeConfigToken);
    }

    /**
     * Returns {@code true} if the token in the given {@link AuthInfo} (that's retrieved from the kube config file)
     * and the new {@link AuthInfo} (that's retrieved from the new kube config file) are equal.
     * Returns {@code false} otherwise.
     *
     * @param authInfo the (kube config) auth info that contains the token
     * @param newAuthInfo the new (kube config) auth that contains the token
     * @return true if both tokens are equal, false otherwise
     */
    public static boolean areEqualToken(AuthInfo authInfo, AuthInfo newAuthInfo) {
        String configToken = getToken(authInfo);
        String newConfigToken = getToken(newAuthInfo);
        if (configToken == null) {
            return newConfigToken == null;
        } else if (newConfigToken == null) {
            return false;
        }

        return configToken.equals(newConfigToken);
    }

    /**
     * Returns the token for the given {@code AuthInfo}. Returns {@code null} if it was not found.
     * The token is searched in the auth provider in the following
     * properties, respecting the given order of precedence:
     * <ul>
     *     <li>"access-token"</li>
     *     <li>"id-token"</li>
     * </ul>
     * @param authInfo the auth info to retrieve the token from
     * @return the token that was found or null
     */
    private static String getToken(AuthInfo authInfo){
        if (authInfo == null) {
            return null;
        }
        if (authInfo.getToken() != null) {
            return authInfo.getToken();
        }
        AuthProviderConfig authProviderConfig = authInfo.getAuthProvider();
        if (authProviderConfig == null) {
            return null;
        }
        Map<String, String> config = authProviderConfig.getConfig();
        if (config == null
            || config.isEmpty()) {
            return null;
        }
        // GKE token
        String accessToken = config.get("access-token");
        if (accessToken != null
            && !accessToken.isEmpty()) {
            return accessToken;
        }
        // OpenID Connect token
        String idToken = config.get("id-token");
        if (idToken != null
            && !idToken.isEmpty()) {
            return idToken;
        }
        return null;
    }

}
