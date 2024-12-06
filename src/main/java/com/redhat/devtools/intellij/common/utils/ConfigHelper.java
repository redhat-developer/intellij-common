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

import io.fabric8.kubernetes.api.model.Context;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.Config;

import java.util.Collection;
import java.util.Objects;

public class ConfigHelper {

    /**
     * Returns {@code true} if the given {@link io.fabric8.kubernetes.client.Config}s are equal.
     * They are considered equal if they're equal in
     * <ul>
     *     <li>current context (cluster, user, current namespace)</li>
     *     <li>auth info</li>
     * </ul>
     *
     * @param thisConfig the first config to compare
     * @param thatConfig the second config to compare
     * @return true if both configs are equal in context, contexts and token
     *
     * @see #areEqualCurrentContext(Config, Config)
     * @see #areEqualCluster(Config, Config)
     * @see #areEqualAuthInfo(Config, Config)
     */
    public static boolean areEqual(Config thisConfig, Config thatConfig) {
        return areEqualCurrentContext(thisConfig, thatConfig)
                && areEqualCluster(thisConfig, thatConfig)
                && areEqualAuthInfo(thisConfig, thatConfig);
    }

    /**
     * Returns {@code true} if the given {@link io.fabric8.kubernetes.client.Config}s are equal in current context.
     * They are considered equal if they're equal in
     * <ul>
     *     <li>name</li>
     *     <li>cluster</li>
     *     <li>user</li>
     *     <li>current namespace</li>
     * </ul>
     *
     * @param thisConfig the first config to compare
     * @param thatConfig the second config to compare
     * @return true if both configs are equal in context, existing contexts and token
     *
     * @see Config#getCurrentContext()
     * @see #areEqualContext(NamedContext, NamedContext)
     */
    public static boolean areEqualCurrentContext(Config thisConfig, Config thatConfig) {
        if (thisConfig == null) {
            return thatConfig == null;
        } else if (thatConfig == null) {
            return false;
        }

        return areEqualContext(thisConfig.getCurrentContext(), thatConfig.getCurrentContext());
    }

    /**
     * Returns {@code true} if both given {@link NamedContext} are equal in
     * <ul>
     *     <li>name</li>
     *     <li>cluster</li>
     *     <li>user</li>
     *     <li>current namespace</li>
     * </ul>
     *
     * @param thisContext the first context to compare
     * @param thatContext the second context to compare
     * @return true if both contexts are equal
     *
     * @see #areEqualContext(Context, Context) 
     * @see NamedContext
     * @see Context
     */
    public static boolean areEqualContext(NamedContext thisContext, NamedContext thatContext) {
        if (thisContext == null) {
            return thatContext == null;
        } else if (thatContext == null) {
            return false;
        }

        return Objects.equals(thisContext.getName(), thatContext.getName())
                && areEqualContext(thisContext.getContext(), thatContext.getContext());
    }

    /**
     * Returns {@code true} if both given {@link Context} are equal.
     * They are considered equal if they're equal in
     * <ul>
     *     <li>cluster</li>
     *     <li>user</li>
     *     <li>current namespace</li>
     * </ul>
     *
     * @param thisContext the first context to compare
     * @param thatContext the second context to compare
     * @return true if both contexts are equal
     *
     * @see NamedContext
     * @see Context
     */
    private static boolean areEqualContext(Context thisContext, Context thatContext) {
        if (thisContext == null) {
            return thatContext == null;
        } else if (thatContext == null) {
            return false;
        }

        return Objects.equals(thisContext.getCluster(), thatContext.getCluster())
                && Objects.equals(thisContext.getUser(), thatContext.getUser())
                && Objects.equals(thisContext.getNamespace(), thatContext.getNamespace());
    }

    /**
     * Returns {@code true} if both given {@link Config} are equal in
     * <ul>
     *     <li>master url</li>
     *     <li>(blindly) trust certificates</li>
     *     <li>proxies</li>
     *     <li>auth info</li>
     * </ul>
     *
     * @param thisConfig the first config to compare
     * @param thatConfig the second config to compare
     * @return true if both configs are equal in master url, trust certs, proxies and auth info
     *
     * @see Config
     */
    public static boolean areEqualCluster(Config thisConfig, Config thatConfig) {
        if (thisConfig == null) {
            return thatConfig == null;
        } else if (thatConfig == null) {
            return false;
        }

        return Objects.equals(thisConfig.getMasterUrl(), thatConfig.getMasterUrl())
                && areEqualTrustCerts(thisConfig, thatConfig)
                && areEqualProxy(thisConfig, thatConfig)
                && areEqualAuthInfo(thisConfig, thatConfig);
    }

    /**
     * Returns {@code true} if both given {@link Config} are equal in
     * <ul>
     *     <li>http proxy</li>
     *     <li>https proxy</li>
     *     <li>proxy username</li>
     *     <li>proxy password</li>
     * </ul>
     *
     * @param thisConfig the first config to compare
     * @param thatConfig the second config to compare
     * @return true if both configs are equal in http- & https-proxy, proxy username & password
     *
     * @see Config
     */
    private static boolean areEqualProxy(Config thisConfig, Config thatConfig) {
        if (thisConfig == null) {
            return thatConfig == null;
        } else if (thatConfig == null) {
            return false;
        }

        return Objects.equals(thisConfig.getHttpProxy(), thatConfig.getHttpProxy())
                && Objects.equals(thisConfig.getHttpsProxy(), thatConfig.getHttpsProxy())
                && Objects.equals(thisConfig.getProxyUsername(), thatConfig.getProxyUsername())
                && Objects.equals(thisConfig.getProxyPassword(), thatConfig.getProxyPassword());
    }

    /**
     * Returns {@code true} if both given {@link Config} are equal in
     * <ul>
     *     <li>(blindly) trusting certificates</li>
     *     <li>disable hostname verification</li>
     *     <li>ca cert data</li>
     *     <li>ca cert file</li>
     * </ul>
     *
     * @param thisConfig the first config to compare
     * @param thatConfig the second config to compare
     * @return true if both configs are equal in trusting certs, disabling hostname verification, ca cert data & file
     *
     * @see Config
     */
    private static boolean areEqualTrustCerts(Config thisConfig, Config thatConfig) {
        if (thisConfig == null) {
            return thatConfig == null;
        } else if (thatConfig == null) {
            return false;
        }

        return thisConfig.isTrustCerts() == thatConfig.isTrustCerts()
                && thisConfig.isDisableHostnameVerification() == thatConfig.isDisableHostnameVerification()
                && Objects.equals(thisConfig.getCaCertData(), thatConfig.getCaCertData())
                && Objects.equals(thisConfig.getCaCertFile(), thatConfig.getCaCertFile());
    }

    /**
     * Returns {@code true} if both given {@link Config} are equal in auth info
     * <ul>
     *     <li>client cert file</li>
     *     <li>client cert data</li>
     *     <li>client key file</li>
     *     <li>client key data</li>
     *     <li>client key algo</li>
     *     <li>username</li>
     *     <li>password</li>
     *     <li>proxies</li>
     *     <li>token</li>
     * </ul>
     *
     * @param thisConfig the first config to compare
     * @param thatConfig the second config to compare
     * @return true if both configs are equal in client cert file/data, key file/data/algo, username, password
     * proxies and token
     *
     * @see Config
     */
    public static boolean areEqualAuthInfo(Config thisConfig, Config thatConfig) {
        if (thisConfig == null) {
            return thatConfig == null;
        } else if (thatConfig == null) {
            return false;
        }

        return Objects.equals(thisConfig.getClientCertFile(), thatConfig.getClientCertFile())
                && Objects.equals(thisConfig.getClientCertData(), thatConfig.getClientCertData())
                && Objects.equals(thisConfig.getClientKeyFile(), thatConfig.getClientKeyFile())
                && Objects.equals(thisConfig.getClientKeyData(), thatConfig.getClientKeyData())
                && Objects.equals(thisConfig.getClientKeyAlgo(), thatConfig.getClientKeyAlgo())
                && Objects.equals(thisConfig.getUsername(), thatConfig.getUsername())
                && Objects.equals(thisConfig.getPassword(), thatConfig.getPassword())
                && areEqualProxy(thisConfig, thatConfig)
                && areEqualToken(thisConfig, thatConfig);
    }

    /**
     * Returns {@code true} if both given {@link Config} are equal in contexts.
     * They are considered equal if they're equal in the number of contexts are these are equal individually.
     * <ul>
     *     <li>cluster</li>
     *     <li>user</li>
     *     <li>current namespace</li>
     *     <li>extensions</li>
     * </ul>
     *
     * @param thisConfig the first context to compare
     * @param thatConfig the second context to compare
     * @return true if both contexts are equal
     *
     * @see NamedContext
     * @see Context
     *
     * @see Config#getContexts()
     */
    public static boolean areEqualContexts(Config thisConfig, Config thatConfig) {
        return areEqualContexts(thisConfig.getContexts(), thatConfig.getContexts());
    }

    /**
     * Returns {@code true} if both given {@link Config} are equal in contexts.
     * They are considered equal if they're equal in the number of contexts are these are equal individually.
     * <ul>
     *     <li>cluster</li>
     *     <li>user</li>
     *     <li>current namespace</li>
     *     <li>extensions</li>
     * </ul>
     *
     * @param these the contexts to compare
     * @param those the other contexts to compare to
     * @return true if both collections of contexts are equal
     *
     * @see NamedContext
     * @see Context
     */
    private static boolean areEqualContexts(Collection<NamedContext> these, Collection<NamedContext> those) {
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
                .anyMatch(named -> areEqualContext(namedContext, named));
    }

    /**
     * Returns {@code true} if the (authentication) token in the given {@link io.fabric8.kubernetes.client.Config}
     * and the one in the other config are equal.
     * Returns {@code false} otherwise.
     *
     * @param thisConfig the config to compare the active token of
     * @param thatConfig the other config to compare the active token of
     * @return true if both tokens are equal, false otherwise
     */
    public static boolean areEqualToken(Config thisConfig, Config thatConfig) {
        if (thisConfig == null) {
            return thatConfig == null;
        } else if (thatConfig == null) {
            return false;
        }

        return Objects.equals(thisConfig.getAutoOAuthToken(), thatConfig.getAutoOAuthToken());
    }
}
