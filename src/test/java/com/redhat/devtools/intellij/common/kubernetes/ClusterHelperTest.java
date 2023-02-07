/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
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
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClusterHelperTest {

    @Test
    public void testKubernetesCluster() {
        VersionInfo versionInfo = mock(VersionInfo.class);
        when(versionInfo.getGitVersion()).thenReturn("1.20.0");
        KubernetesClient client = mock(KubernetesClient.class);
        when(client.getKubernetesVersion()).thenReturn(versionInfo);
        ClusterInfo info = ClusterHelper.getClusterInfo(client);
        assertEquals("1.20.0", info.getKubernetesVersion());
        assertFalse(info.isOpenshift());
        assertEquals("", info.getOpenshiftVersion());
    }

    @Test
    public void testOCP4NormalUserCluster() {
        OpenShiftClient oclient = mock(OpenShiftClient.class);
        when(oclient.getVersion()).thenReturn(null);
        when(oclient.isSupported()).thenReturn(true);
        VersionInfo versionInfo = mock(VersionInfo.class);
        when(versionInfo.getGitVersion()).thenReturn("1.20.0");
        KubernetesClient client = mock(KubernetesClient.class);
        when(client.adapt(OpenShiftClient.class)).thenReturn(oclient);
        when(client.getKubernetesVersion()).thenReturn(versionInfo);
        ClusterInfo info = ClusterHelper.getClusterInfo(client);
        assertEquals("1.20.0", info.getKubernetesVersion());
        assertTrue(info.isOpenshift());
        assertEquals("", info.getOpenshiftVersion());
    }

    @Test
    public void testOCP3OrOCP4AdminUserCluster() {
        VersionInfo oversionInfo = mock(VersionInfo.class);
        when(oversionInfo.getMajor()).thenReturn("4");
        when(oversionInfo.getMinor()).thenReturn("7.0");
        OpenShiftClient oclient = mock(OpenShiftClient.class);
        when(oclient.getVersion()).thenReturn(oversionInfo);
        when(oclient.isSupported()).thenReturn(true);
        VersionInfo versionInfo = mock(VersionInfo.class);
        when(versionInfo.getGitVersion()).thenReturn("1.20.0");
        KubernetesClient client = mock(KubernetesClient.class);
        when(client.adapt(OpenShiftClient.class)).thenReturn(oclient);
        when(client.getKubernetesVersion()).thenReturn(versionInfo);
        ClusterInfo info = ClusterHelper.getClusterInfo(client);
        assertEquals("1.20.0", info.getKubernetesVersion());
        assertTrue(info.isOpenshift());
        assertEquals("4.7.0", info.getOpenshiftVersion());
    }
}
