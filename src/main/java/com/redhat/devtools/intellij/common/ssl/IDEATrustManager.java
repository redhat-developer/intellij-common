/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.ssl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.net.ssl.CertificateManager;
import nl.altindag.ssl.trustmanager.CompositeX509ExtendedTrustManager;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IDEATrustManager {

    private static final Logger LOG = Logger.getInstance(IDEATrustManager.class);

    private final X509TrustManager trustManager;


    public IDEATrustManager(){
         trustManager = CertificateManager.getInstance().getTrustManager();
    }
    public IDEATrustManager(X509TrustManager trustManager) {
        this.trustManager = trustManager;
    }

    public X509TrustManager configure(List<X509ExtendedTrustManager> toAdd) {
        try {
            if (hasSystemManagerField()) {
                // < IC-2022.2
                setCompositeManager(toAdd, trustManager);
            } else {
                // >= IC-2022.2
                addCompositeManager(toAdd, trustManager);
            }
        } catch (RuntimeException | IllegalAccessException e) {
            LOG.warn("Could not configure IDEA trust manager.", e);
        }
        return trustManager;
    }

    /**
     * Returns `true` if [ConfirmingTrustManager] has a private field `mySystemManager`.
     * Returns `false` otherwise.
     * IDEA < IC-2022.2 manages a single [X509TrustManager] in a private field called `mySystemManager`.
     * IDEA >= IC-2022.2 manages a list of [X509TrustManager]s in a private list called `mySystemManagers`.
     *
     * @return true if com.intellij.util.net.ssl.ConfirmingTrustManager has a field mySystemManager. False otherwise.
     */
    private boolean hasSystemManagerField() {
        return getSystemManagerField() != null;
    }

    private Field getSystemManagerField() {
        return FieldUtils.getDeclaredField(
            trustManager.getClass(),
            "mySystemManager",
            true
        );
    }

    /**
     * Sets a [CompositeX509ExtendedTrustManager] with the given [X509TrustManager]s
     * to the given destination [X509TrustManager].
     * If a [CompositeX509ExtendedTrustManager] already exists, his first entry is taken and set to a new
     * [CompositeX509ExtendedTrustManager] that replaces the existing one.
     *
     * @param trustManagers the trust managers that should be set to the destination trust manager
     * @param destination   the destination trust manager that should receive the trust managers
     */
    private void setCompositeManager(
        List<X509ExtendedTrustManager> trustManagers,
        X509TrustManager destination
    ) throws IllegalAccessException {
        Field systemManagerField = getSystemManagerField();
        if (systemManagerField == null)
            return;
        Object object = systemManagerField.get(destination);
        if (!(object instanceof X509ExtendedTrustManager)) {
            return;
        }
        X509ExtendedTrustManager systemManager = (X509ExtendedTrustManager) object;
        X509ExtendedTrustManager compositeTrustManager = createCompositeTrustManager(systemManager, trustManagers);
        systemManagerField.set(destination, compositeTrustManager);
    }

    private X509ExtendedTrustManager createCompositeTrustManager(
        X509ExtendedTrustManager systemManager,
        List<X509ExtendedTrustManager> clientTrustManagers
    ) {
        List<X509ExtendedTrustManager> trustManagers = new ArrayList<>();
        if (systemManager instanceof CompositeX509ExtendedTrustManager) {
            // already patched CertificateManager, take 1st entry in existing system manager
            trustManagers.add(((CompositeX509ExtendedTrustManager) systemManager).getInnerTrustManagers().get(0));
        } else {
            // unpatched CertificateManager, take system manager
            trustManagers.add(systemManager);
        }
        trustManagers.addAll(clientTrustManagers);
        return new CompositeX509ExtendedTrustManager(trustManagers);
    }

    /**
     * Adds a [CompositeX509ExtendedTrustManager] to the given destination [X509TrustManager].
     * If a [CompositeX509ExtendedTrustManager] already exists, it is replaced by a new [CompositeX509ExtendedTrustManager].
     *
     * @param trustManagers the trust managers that should be added to destination trust manager
     * @param destination   the trust manager that should receive the given trust managers
     */
    private void addCompositeManager(
        List<X509ExtendedTrustManager> trustManagers,
        X509TrustManager destination
    ) throws IllegalAccessException {
        Field systemManagersField = FieldUtils.getDeclaredField(
            destination.getClass(),
            "mySystemManagers",
            true);
        if (systemManagersField == null) {
            return;
        }
        Object object = systemManagersField.get(destination);
        if (!(object instanceof List))
            return;
        List<X509TrustManager> managers = (List<X509TrustManager>) object;
        List<X509TrustManager> nonCompositeManagers = managers.stream().filter(x509TrustManager -> !(x509TrustManager instanceof CompositeX509ExtendedTrustManager)).collect(Collectors.toList());
        CompositeX509ExtendedTrustManager clientTrustManager = new CompositeX509ExtendedTrustManager(new ArrayList<>(trustManagers));
        managers.clear();
        managers.addAll(nonCompositeManagers);
        managers.add(clientTrustManager);
    }
}