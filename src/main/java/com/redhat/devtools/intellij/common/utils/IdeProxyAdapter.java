/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class IdeProxyAdapter {

    public static ProxySelector getProxySelector() {
        ProxySelector selector = Pre243.getProxySelector();
        if (selector != null) {
            return selector;
        } else {
            return Post243.getProxySelector();
        }
    }

    public static Authenticator getAuthenticator() {
        Authenticator authenticator = Pre243.getAuthenticator();
        if (authenticator != null) {
            return authenticator;
        } else {
            return Post243.getAuthenticator();
        }
    }

    public static List<Proxy> getProxies(String url) throws URISyntaxException {
        List<Proxy> proxies = Pre243.getProxies(url);
        if (proxies != null
                && proxies.isEmpty()) {
            proxies = Post243.getProxies(url);
        }
        return proxies;
    }

    public static PasswordAuthentication getPasswordAuthentication() {
        PasswordAuthentication authentication = Pre243.getPasswordAuthentication();
        if (authentication != null) {
            return authentication;
        } else {
            return Post243.getPasswordAuthentication();
        }
    }

    private static class Pre243 {

        static ProxySelector getProxySelector() {
            Object httpConfigurable = httpConfigurable_getInstance();
            if (httpConfigurable == null) {
                return null;
            }
            return (ProxySelector) ideaWideProxySelector_newInstance(httpConfigurable);
        }

        /**
         * new IdeaWideAuthenticator(HttpConfigurable)
         */
        static Authenticator getAuthenticator() {
            Object httpConfigurable = httpConfigurable_getInstance();
            if (httpConfigurable == null) {
                return null;
            }
            return (Authenticator) ideaWideAuthenticator_newInstance(httpConfigurable);
        }

        static List<Proxy> getProxies(String url) {
            List<Proxy> proxies = new ArrayList<>();
            try {
                Object httpConfigurable = httpConfigurable_getInstance();
                if (httpConfigurable != null) {
                    Object ideaWideProxySelector = ideaWideProxySelector_newInstance(httpConfigurable);
                    if (ideaWideProxySelector != null) {
                        proxies = ideaWideProxySelector_select(ideaWideProxySelector, new URI(url));
                    }
                }
            } catch (URISyntaxException e) {
                // swallow only
            }
            return proxies;
        }

        /**
         * new IdeaWideAuthenticator(HttpConfigurable.getInstance()).getPasswordAuthentication()
         */
        static private PasswordAuthentication getPasswordAuthentication() {
            PasswordAuthentication authentication = null;
            Object httpConfigurable = httpConfigurable_getInstance();
            if (httpConfigurable != null) {
                Object ideaWideAuthenticator = ideaWideAuthenticator_newInstance(httpConfigurable);
                if (ideaWideAuthenticator != null) {
                    authentication = ideaWideAuthenticator_getPasswordAuthentication(ideaWideAuthenticator);
                }
            }
            return authentication;
        }

        /**
         * IdeaWideProxySelector.select(URI)
         */
        @SuppressWarnings("unchecked")
        private static List<Proxy> ideaWideProxySelector_select(Object ideaWideProxySelector, URI uri) {
            try {
                Method method = ideaWideProxySelector.getClass().getDeclaredMethod("select", URI.class);
                return (List<Proxy>) method.invoke(ideaWideProxySelector, uri);
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                return null;
            }
        }

        /**
         * new IdeaWideProxySelector()
         */
        @SuppressWarnings("JavaReflectionMemberAccess")
        private static Object ideaWideProxySelector_newInstance(Object httpConfigurable) {
            try {
                Class<?> clazz = Class.forName("com.intellij.util.net.IdeaWideProxySelector");
                Constructor<?> constructor = clazz.getConstructor(httpConfigurable.getClass());
                return constructor.newInstance(httpConfigurable);
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                return null;
            }
        }

        /**
         * new IdeaWideAuthenticator(HttpConfigurable)
         */
        @SuppressWarnings("JavaReflectionMemberAccess")
        private static Object ideaWideAuthenticator_newInstance(Object httpConfigurable) {
            try {
                Class<?> clazz = Class.forName("com.intellij.util.net.IdeaWideAuthenticator");
                Constructor<?> constructor = clazz.getConstructor(httpConfigurable.getClass());
                return constructor.newInstance(httpConfigurable);
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                return null;
            }
        }

        /**
         * IdeaWideAuthenticator.getPasswordAuthentication()
         */
        private static PasswordAuthentication ideaWideAuthenticator_getPasswordAuthentication(Object ideaWideAuthenticator) {
            try {
                Method method = ideaWideAuthenticator.getClass().getDeclaredMethod("getPasswordAuthentication");
                return (PasswordAuthentication) method.invoke(ideaWideAuthenticator);
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                return null;
            }
        }

        /**
         * HttpConfigurable.getInstance()
         */
        private static Object httpConfigurable_getInstance() {
            try {
                Class<?> clazz = Class.forName("com.intellij.util.net.HttpConfigurable");
                Method getInstanceMethod = clazz.getDeclaredMethod("getInstance");
                return getInstanceMethod.invoke(null);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
    }

    private static class Post243 {

        /**
         * JdkProxyProvider.getInstance().getProxySelector()
         */
        static ProxySelector getProxySelector() {
            Object provider = jdkProxyProvider_getInstance();
            if (provider == null) {
                return null;
            }
            return (ProxySelector) jdkProxyProvider_getProxySelector(provider.getClass());
        }

        /**
         * JdkProxyProvider.getInstance().getAuthenticator()
         */
        public static Authenticator getAuthenticator() {
            Object provider = jdkProxyProvider_getInstance();
            if (provider == null) {
                return null;
            }
            return (Authenticator) jdkProxyProvider_getAuthenticator(provider.getClass());
        }

        /**
         * JdkProxyProvider.getInstance().getProxySelector().select(URI.create(url))
         */
        static List<Proxy> getProxies(String url) {
            List<Proxy> proxies = new ArrayList<>();
            try {
                ProxySelector selector = getProxySelector();
                if (selector != null) {
                    proxies = proxySelector_select(selector, new URI(url));
                }
            } catch (URISyntaxException e) {
                // swallow only
            }
            return proxies;
        }

        /**
         * JdkProxyProvider.getInstance().getAuthenticator().requestPasswordAuthentication(
         *             null,
         *             0,
         *             null,
         *             null,
         *             null
         *         );
         */
        private static PasswordAuthentication getPasswordAuthentication() {
            PasswordAuthentication authentication = null;
            Object provider = jdkProxyProvider_getInstance();
            if (provider != null) {
                Object authenticator = jdkProxyProvider_getAuthenticator(provider.getClass());
                if (authenticator != null) {
                    authentication = authenticator_requestPasswordAuthentication(authenticator);
                }
            }
            return authentication;
        }

        /**
         * ProxySelector.select(URI)
         */
        @SuppressWarnings("unchecked")
        private static List<Proxy> proxySelector_select(Object proxySelector, URI uri) {
            try {
                Method method = proxySelector.getClass().getDeclaredMethod("select", URI.class);
                return (List<Proxy>) method.invoke(proxySelector, uri);
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                return null;
            }
        }

        /**
         * ProxySelector.select(URI)
         */
        private static PasswordAuthentication authenticator_requestPasswordAuthentication(Object authenticator) {
            try {
                Method method = authenticator.getClass().getDeclaredMethod("requestPasswordAuthentication",
                        InetAddress.class,
                        Integer.TYPE,
                        String.class,
                        String.class,
                        String.class);
                return (PasswordAuthentication) method.invoke(authenticator,
                        null,
                        0,
                        null,
                        null,
                        null
                );
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                return null;
            }
        }

        /**
         * JdkProxyProvider.getProxySelector()
         */
        private static Object jdkProxyProvider_getProxySelector(Class<?> jdkProxyProviderClass) {
            try {
                Method getInstanceMethod = jdkProxyProviderClass.getDeclaredMethod("getProxySelector");
                return getInstanceMethod.invoke(null);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        /**
         * JdkProxyProvider.getProxySelector()
         */
        private static Object jdkProxyProvider_getAuthenticator(Class<?> jdkProxyProviderClass) {
            try {
                Method getInstanceMethod = jdkProxyProviderClass.getDeclaredMethod("getAuthenticator");
                return getInstanceMethod.invoke(null);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        /**
         * JdkProxyProvider.getInstance()
         */
        private static Object jdkProxyProvider_getInstance() {
            try {
                Class<?> clazz = Class.forName("com.intellij.util.net.JdkProxyProvider");
                Method getInstanceMethod = clazz.getDeclaredMethod("getInstance");
                return getInstanceMethod.invoke(null);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
    }
}
