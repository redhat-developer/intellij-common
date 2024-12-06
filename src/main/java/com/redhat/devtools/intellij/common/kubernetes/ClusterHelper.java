/*******************************************************************************
 * Copyright (c) 2021-2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.openshift.client.OpenShiftClient;

public class ClusterHelper {

    private ClusterHelper() {
        //avoid instanciation
    }

    public static boolean isOpenShift(KubernetesClient client) {
        try {
            return client != null
                    && client.hasApiGroup(OpenShiftClient.BASE_API_GROUP, false);
        } catch (KubernetesClientException e) {
            return false;
        }
    }

    public static ClusterInfo getClusterInfo(KubernetesClient client) {
        OpenShiftClient openShiftClient = getOpenShiftClient(client);
        if (openShiftClient != null) {
            return new ClusterInfo(
                    getKubernetesVersion(client),
                    true,
                    getOpenShiftVersion(openShiftClient));
        } else {
            return new ClusterInfo(
                    getKubernetesVersion(client),
                    false,
                    "");
        }
    }

    private static String getKubernetesVersion(KubernetesClient client) {
        VersionInfo version = client.getKubernetesVersion();
        return version != null ? version.getGitVersion() : "";
    }

    private static OpenShiftClient getOpenShiftClient(KubernetesClient client) {
        if (client instanceof OpenShiftClient) {
            return (OpenShiftClient) client;
        } else if (isOpenShift(client)) {
            return client.adapt(OpenShiftClient.class);
        } else {
            return null;
        }
    }

    private static String getOpenShiftVersion(OpenShiftClient client) {
        VersionInfo version = client.getVersion();
        if (version != null && version.getMajor() != null) {
            return getVersion(version.getMajor(), version.getMinor());
        } else {
            return "";
        }
    }

    private static String getVersion(String major, String minor) {
        return major + '.' + minor;
    }

}
