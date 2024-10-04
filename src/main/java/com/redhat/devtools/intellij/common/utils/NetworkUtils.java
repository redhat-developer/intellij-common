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
package com.redhat.devtools.intellij.common.utils;

import com.google.common.collect.Sets;
import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static okhttp3.Credentials.basic;

public class NetworkUtils {

    public static OkHttpClient getClient() {
        final ProxySelector proxySelector = IdeProxyAdapter.getProxySelector();
        final Authenticator proxyAuthenticator = getProxyAuthenticator(IdeProxyAdapter.getPasswordAuthentication());
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.proxySelector(proxySelector)
                .proxyAuthenticator(proxyAuthenticator);

        return builder.build();
    }

    private static Authenticator getProxyAuthenticator(PasswordAuthentication authentication) {
        Authenticator proxyAuthenticator = null;

        if (authentication != null) {
            proxyAuthenticator = (route, response) -> {
                final String credential = basic(authentication.getUserName(), Arrays.toString(authentication.getPassword()));
                return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
            };
        }

        return proxyAuthenticator;
    }

    @NotNull
    public static Map<String, String> buildEnvironmentVariables(String url) throws URISyntaxException {
        final Map<String, String> environmentVariables = new HashMap<>(6);

        final List<Proxy> proxies = IdeProxyAdapter.getProxies(url);
        final Proxy proxy = proxies.stream().findFirst().orElse(null);
        if (proxy != null) {
            final Proxy.Type type = proxy.type();

            switch (type) {
                case HTTP:
                case SOCKS:
                    final SocketAddress address = proxy.address();
                    if (address instanceof InetSocketAddress) {
                        final PasswordAuthentication authentication = IdeProxyAdapter.getPasswordAuthentication();
                        final String envVarValue = buildHttpProxy(type, authentication, (InetSocketAddress) address);
                        final Set<String> proxyEnvironmentVariables = Sets.newHashSet("HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY");
                        proxyEnvironmentVariables.forEach(envVarName -> {
                            environmentVariables.put(envVarName, envVarValue);
                            environmentVariables.put(envVarName.toLowerCase(), envVarValue);
                        });
                    }
                    break;
            }
        }

        return environmentVariables;
    }

    @NotNull
    private static String buildHttpProxy(Proxy.Type type, PasswordAuthentication authentication, InetSocketAddress address) {
        final InetAddress inetAddress = address.getAddress();
        final String host = inetAddress.getHostAddress();
        final int port = address.getPort();
        return buildHttpProxy(type, authentication, host, port);
    }

    @NotNull
    private static String buildHttpProxy(Proxy.Type type, PasswordAuthentication authentication, String host, int port) {
        String userName = null;
        String password = null;
        if (authentication != null) {
            userName = authentication.getUserName();
            password = Arrays.toString(authentication.getPassword());
        }
        return buildHttpProxy(type, userName, password, host, port);
    }

    @NotNull
    private static String buildHttpProxy(Proxy.Type type, String userName, String password, String host, int port) {
        final StringBuilder builder = new StringBuilder();

        switch (type) {
            case HTTP:
                builder.append("http://");
                break;
            case SOCKS:
                builder.append("socks://");
                break;
        }

        if (isNotEmpty(userName) && isNotEmpty(password)) {
            builder.append(userName).append(":").append(password).append("@");
        }

        builder.append(host).append(":").append(port);

        return builder.toString();
    }
}
