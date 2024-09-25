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

    /**
     *
     * Returns the proxies that exist for the given url.
     *
     * @param url the url to get proxies for
     * @return the proxies for the given url
     *
     */
    public static List<Proxy> getProxies(String url) {
        List<Proxy> proxies = Pre243.getProxies(url);
        if (proxies != null
                && !proxies.isEmpty()) {
            return proxies;
        } else {
            return Post243.getProxies(url);
        }
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
         * <code>
         *  new IdeaWideAuthenticator(HttpConfigurable)
         * </code>
         */
        static Authenticator getAuthenticator() {
            Object commonProxy = commonProxy_getInstance();
            if (commonProxy == null) {
                return null;
            }
            return commonProxy_getAuthenticator(commonProxy);
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
         * <code>
         *  new IdeaWideAuthenticator(HttpConfigurable.getInstance()).getPasswordAuthentication()
         * </code>
         */
        static private PasswordAuthentication getPasswordAuthentication() {
            PasswordAuthentication authentication = null;
            Authenticator authenticator = getAuthenticator();
            if (authenticator != null) {
                authentication = authenticator_getPasswordAuthentication(authenticator);
            }
            return authentication;
        }

        /**
         * <code>
         *  IdeaWideProxySelector.select(URI)
         * </code>
         */
        @SuppressWarnings("unchecked")
        private static List<Proxy> ideaWideProxySelector_select(Object ideaWideProxySelector, URI uri) {
            try {
                Method method = ideaWideProxySelector.getClass().getDeclaredMethod("select", URI.class);
                return (List<Proxy>) method.invoke(ideaWideProxySelector, uri);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * <code>
         *  new IdeaWideProxySelector()
         * </code>
         */
        @SuppressWarnings("JavaReflectionMemberAccess")
        private static Object ideaWideProxySelector_newInstance(Object httpConfigurable) {
            try {
                Class<?> clazz = Class.forName("com.intellij.util.net.IdeaWideProxySelector");
                Constructor<?> constructor = clazz.getConstructor(httpConfigurable.getClass());
                return constructor.newInstance(httpConfigurable);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * <code>
         *  new IdeaWideAuthenticator(HttpConfigurable)
         * </code>
         */
        private static Object ideaWideAuthenticator_newInstance(Object httpConfigurable) {
            try {
                Class<?> clazz = httpConfigurable.getClass();
                Constructor<?> constructor = clazz.getConstructor(httpConfigurable.getClass());
                return constructor.newInstance(httpConfigurable);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * <code>
         *  HttpConfigurable.getInstance()
         * </code>
         */
        private static Object httpConfigurable_getInstance() {
            try {
                Class<?> clazz = Class.forName("com.intellij.util.net.HttpConfigurable");
                Method getInstanceMethod = clazz.getDeclaredMethod("getInstance");
                return getInstanceMethod.invoke(null);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * <code>
         *  CommonProxy.getInstance()
         * </code>
         */
        private static Object commonProxy_getInstance() {
            try {
                Class<?> clazz = Class.forName("com.intellij.util.proxy.CommonProxy");
                Method getInstanceMethod = clazz.getDeclaredMethod("getInstance");
                return getInstanceMethod.invoke(null);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        private static Authenticator commonProxy_getAuthenticator(Object commonProxy) {
            try {
                Class<?> clazz = commonProxy.getClass();
                Method method = clazz.getDeclaredMethod("getAuthenticator");
                return (Authenticator) method.invoke(commonProxy);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
    }

    private static class Post243 {

        /**
         * <code>
         *  JdkProxyProvider.getInstance().getProxySelector()
         * </code>
         */
        public static ProxySelector getProxySelector() {
            Object provider = jdkProxyProvider_getInstance();
            if (provider == null) {
                return null;
            }
            return jdkProxyProvider_getProxySelector(provider);
        }

        /**
         * <code>
         *  JdkProxyProvider.getInstance().getAuthenticator()
         * </code>
         */
        public static Authenticator getAuthenticator() {
            Object proxyAuthentication = jdkProxyProvider_getInstance();
            if (proxyAuthentication == null) {
                return null;
            }
            return jdkProxyProvider_getAuthenticator(proxyAuthentication);
        }

        /**
         * <code>
         *  JdkProxyProvider.getInstance().getProxySelector().select(URI.create(url))
         * </code>
         */
        public static List<Proxy> getProxies(String url) {
            List<Proxy> proxies = new ArrayList<>();
            try {
                ProxySelector selector = getProxySelector();
                if (selector != null) {
                    proxies = proxySelector_select(selector, new URI(url));
                }
            } catch (Exception e) {
                // swallow only
            }
            return proxies;
        }

        /**
         * <code>
         *  JdkProxyProvider.getInstance().getAuthenticator().getPasswordAuthentication();
         * </code>
         */
        private static PasswordAuthentication getPasswordAuthentication() {
            PasswordAuthentication authentication = null;
            Authenticator authenticator = getAuthenticator();
            if (authenticator != null) {
                authentication = authenticator_getPasswordAuthentication(authenticator);
            }
            return authentication;
        }

        /**
         * <code>
         *  ProxySelector.select(URI)
         * <code>
         */
        @SuppressWarnings("unchecked")
        private static List<Proxy> proxySelector_select(Object proxySelector, URI uri) {
            try {
                Method method = proxySelector.getClass().getDeclaredMethod("select", URI.class);
                method.setAccessible(true);
                return (List<Proxy>) method.invoke(proxySelector, uri);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * <code>
         *     JdkProxyProvider.getProxySelector()
         * </code>
         */
        private static ProxySelector jdkProxyProvider_getProxySelector(Object jdkProxyProvider) {
            try {
                Method getInstanceMethod = jdkProxyProvider.getClass().getDeclaredMethod("getProxySelector");
                return (ProxySelector) getInstanceMethod.invoke(jdkProxyProvider);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * <code>
         *  JdkProxyProvider.getAuthenticator()
         * </code>
         */
        private static Authenticator jdkProxyProvider_getAuthenticator(Object jdkProxyProvider) {
            try {
                Method getAuthenticatorMethod = jdkProxyProvider.getClass().getDeclaredMethod("getAuthenticator");
                return (Authenticator) getAuthenticatorMethod.invoke(jdkProxyProvider);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * <code>
         *  JdkProxyProvider.getInstance()
         * </code>
         */
        private static Object jdkProxyProvider_getInstance() {
            try {
                Class<?> clazz = Class.forName("com.intellij.util.net.JdkProxyProvider");
                Method getInstanceMethod = clazz.getDeclaredMethod("getInstance");
                return getInstanceMethod.invoke(null);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * <code>
     *  Authenticator.getPasswordAuthentication()
     * </code>
     */
    private static PasswordAuthentication authenticator_getPasswordAuthentication(Object authenticator) {
        try {
            Method method = authenticator.getClass().getDeclaredMethod("getPasswordAuthentication");
            method.setAccessible(true);
            return (PasswordAuthentication) method.invoke(authenticator);
        } catch (Exception e) {
            return null;
        }
    }
}
