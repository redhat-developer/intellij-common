/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Based on nl.altindag.ssl.trustmanager.CompositeX509ExtendedTrustManager at https://github.com/Hakky54/sslcontext-kickstart
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.ssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CompositeX509ExtendedTrustManager extends X509ExtendedTrustManager {

    private static final String CERTIFICATE_EXCEPTION_MESSAGE = "None of the TrustManagers trust this certificate chain";

    private final List<X509ExtendedTrustManager> innerTrustManagers;
    private final X509Certificate[] acceptedIssuers;

    public CompositeX509ExtendedTrustManager(List<X509ExtendedTrustManager> trustManagers) {
        this.innerTrustManagers = Collections.unmodifiableList(trustManagers);
        this.acceptedIssuers = trustManagers.stream()
            .map((manager) ->
                Objects.requireNonNullElseGet(manager.getAcceptedIssuers(), () -> new X509Certificate[]{})
            )
            .flatMap(Arrays::stream)
            .toArray(X509Certificate[]::new);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return Arrays.copyOf(acceptedIssuers, acceptedIssuers.length);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkTrusted((trustManager) -> trustManager.checkClientTrusted(chain, authType));
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkTrusted((trustManager) -> trustManager.checkServerTrusted(chain, authType));
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        checkTrusted((trustManager) -> trustManager.checkClientTrusted(chain, authType, socket));
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        checkTrusted((trustManager) -> trustManager.checkServerTrusted(chain, authType, socket));
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        checkTrusted((trustManager) -> trustManager.checkClientTrusted(chain, authType, engine));
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        checkTrusted((trustManager) -> trustManager.checkServerTrusted(chain, authType, engine));
    }

    public List<X509ExtendedTrustManager> getInnerTrustManagers() {
        return innerTrustManagers;
    }

    private void checkTrusted(TrustManagerConsumer consumer) throws CertificateException {
        List<CertificateException> certificateExceptions = new ArrayList<>();
        for (X509ExtendedTrustManager trustManager : innerTrustManagers) {
            try {
                consumer.checkTrusted(trustManager);
                return;
            } catch (CertificateException e) {
                certificateExceptions.add(e);
            } catch (RuntimeException e) {
              Throwable cause = e.getCause();
              if (!(cause instanceof InvalidAlgorithmParameterException)) {
                throw e;
              }

              certificateExceptions.add(new CertificateException(cause));
            }
        }
        CertificateException certificateException = new CertificateException(CERTIFICATE_EXCEPTION_MESSAGE);
        certificateExceptions.forEach(certificateException::addSuppressed);
        throw certificateException;
    }

    interface TrustManagerConsumer {
        void checkTrusted(X509ExtendedTrustManager var1) throws CertificateException;
    }
}
