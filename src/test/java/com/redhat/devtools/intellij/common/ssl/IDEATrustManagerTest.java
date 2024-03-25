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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IDEATrustManagerTest {

    @Test
    public void single_system_manager_field_should_replace_existing_trust_manager_with_new_composite_trust_manager() {
        // given
        TrustManagerWithMySystemManagerField trustManager = new TrustManagerWithMySystemManagerField(mock(X509ExtendedTrustManager.class));
        IDEATrustManager operator = new IDEATrustManager(trustManager);
        assertThat(trustManager.mySystemManager)
            .isNotInstanceOf(CompositeX509ExtendedTrustManager.class);
        // when
        operator.configure(Collections.emptyList());
        // then
        assertThat(trustManager.mySystemManager)
            .isInstanceOf(CompositeX509ExtendedTrustManager.class);
    }

    @Test
    public void single_system_manager_field_should_replace_existing_trust_manager_with_new_composite_trust_manager_that_contains_given_trust_managers() {
        // given
        TrustManagerWithMySystemManagerField trustManager = new TrustManagerWithMySystemManagerField(mock(X509ExtendedTrustManager.class));
        IDEATrustManager operator = new IDEATrustManager(trustManager);
        List<X509ExtendedTrustManager> newTrustManagers = Arrays.asList(
            mock(X509ExtendedTrustManager.class),
            mock(X509ExtendedTrustManager.class)
        );
        // when
        operator.configure(newTrustManagers);
        // then
        assertThat(trustManager.mySystemManager)
            .isInstanceOf(CompositeX509ExtendedTrustManager.class);
        List<X509ExtendedTrustManager> afterConfigure = ((CompositeX509ExtendedTrustManager)trustManager.mySystemManager).getInnerTrustManagers();
        assertThat(afterConfigure)
            .containsAll(newTrustManagers); // new instance contains list given to configure()
    }

    @Test
    public void single_system_manager_field_should_replace_existing_trust_manager_with_new_composite_trust_manager_that_has_replaced_trust_manager_as_1st_entry() {
        // given
        X509ExtendedTrustManager beforeReplace = mock(X509ExtendedTrustManager.class);
        TrustManagerWithMySystemManagerField trustManager = new TrustManagerWithMySystemManagerField(beforeReplace);
        IDEATrustManager operator = new IDEATrustManager(trustManager);
        // when
        operator.configure(
            Arrays.asList(
                mock(X509ExtendedTrustManager.class),
                mock(X509ExtendedTrustManager.class)
            )
        );
        // then
        assertThat(trustManager.mySystemManager)
            .isInstanceOf(CompositeX509ExtendedTrustManager.class);
        List<X509ExtendedTrustManager> afterConfigure = ((CompositeX509ExtendedTrustManager)trustManager.mySystemManager).getInnerTrustManagers();
        assertThat(afterConfigure.get(0)) // new instance contains 1st entry of replaced instance
            .isEqualTo(beforeReplace);
    }

    @Test
    public void single_system_manager_field_should_replace_composite_trust_manager_with_new_instance_that_has_1st_entry_of_replaced_composite_manager() {
        // given
        X509ExtendedTrustManager toInclude = mock(X509ExtendedTrustManager.class);
        X509ExtendedTrustManager toExclude = mock(X509ExtendedTrustManager.class);
        CompositeX509ExtendedTrustManager compositeTrustManager = new CompositeX509ExtendedTrustManager(Arrays.asList(toInclude, toExclude));
        TrustManagerWithMySystemManagerField trustManager = new TrustManagerWithMySystemManagerField(compositeTrustManager);
        IDEATrustManager manager = new IDEATrustManager(trustManager);
        // when
        manager.configure(
            Arrays.asList(
                mock(X509ExtendedTrustManager.class),
                mock(X509ExtendedTrustManager.class)
            )
        );
        // then
        assertThat(trustManager.mySystemManager)
            .isNotSameAs(compositeTrustManager) // a new instance was created
            .isInstanceOf(CompositeX509ExtendedTrustManager.class);
        List<X509ExtendedTrustManager> afterConfigure = ((CompositeX509ExtendedTrustManager)trustManager.mySystemManager).getInnerTrustManagers();
        assertThat(afterConfigure.get(0)) // new instance contains 1st entry of replaced instance
            .isEqualTo(toInclude);
    }

    @Test
    public void multi_system_managers_field_should_still_contain_existing_trust_managers() {
        // given
        X509ExtendedTrustManager existing = mock(X509ExtendedTrustManager.class);
        List<X509TrustManager> managers = Collections.singletonList(existing);
        TrustManagerWithMySystemManagersField trustManager = new TrustManagerWithMySystemManagersField(managers);
        IDEATrustManager operator = new IDEATrustManager(trustManager);
        // when
        operator.configure(Collections.emptyList());
        // then
        assertThat(trustManager.mySystemManagers)
            .contains(existing);
    }

    @Test
    public void multi_system_managers_field_should_add_composite_manager_that_contains_new_trust_managers() {
        // given
        List<X509TrustManager> managers = new ArrayList<>();
        managers.add(mock(X509ExtendedTrustManager.class));
        TrustManagerWithMySystemManagersField trustManager = new TrustManagerWithMySystemManagersField(managers);
        IDEATrustManager operator = new IDEATrustManager(trustManager);
        List<X509ExtendedTrustManager> newTrustManagers = Arrays.asList(
            mock(X509ExtendedTrustManager.class),
            mock(X509ExtendedTrustManager.class)
        );
        // when
        operator.configure(newTrustManagers);
        // then
        Optional<CompositeX509ExtendedTrustManager> composite = trustManager.mySystemManagers.stream().filter(CompositeX509ExtendedTrustManager.class::isInstance).map(CompositeX509ExtendedTrustManager.class::cast ).findFirst();
        assertTrue(composite.isPresent());
        assertThat(composite.get().getInnerTrustManagers()).containsAll(newTrustManagers);
    }

    @Test
    public void multi_system_managers_field_should_replace_existing_composite_manager_that_contains_new_trust_managers() {
        // given
        X509ExtendedTrustManager existingTrustManager = mock(X509ExtendedTrustManager.class);
        CompositeX509ExtendedTrustManager existingCompositeManager = new CompositeX509ExtendedTrustManager(Collections.singletonList(mock(X509ExtendedTrustManager.class)));
        List<X509TrustManager> managers = new ArrayList<>();
        managers.add(existingTrustManager);
        managers.add(existingCompositeManager);
        TrustManagerWithMySystemManagersField trustManager = new TrustManagerWithMySystemManagersField(managers);
        IDEATrustManager operator = new IDEATrustManager(trustManager);
        List<X509ExtendedTrustManager> newTrustManagers = Arrays.asList(
            mock(X509ExtendedTrustManager.class),
            mock(X509ExtendedTrustManager.class)
        );
        // when
        operator.configure(newTrustManagers);
        // then
        assertThat(trustManager.mySystemManagers).doesNotContain(existingCompositeManager);
        Optional<CompositeX509ExtendedTrustManager> composite = trustManager.mySystemManagers.stream().filter(CompositeX509ExtendedTrustManager.class::isInstance).map(CompositeX509ExtendedTrustManager.class::cast ).findFirst();
        assertTrue(composite.isPresent());
        assertThat(composite.get().getInnerTrustManagers()).containsAll(newTrustManagers);
    }

    /** [com.intellij.util.net.ssl.ConfirmingTrustManager] in < IC-2022.2 */
    private static class TrustManagerWithMySystemManagerField implements X509TrustManager {

        X509TrustManager mySystemManager;

        public TrustManagerWithMySystemManagerField(X509TrustManager mySystemManager) {
            this.mySystemManager = mySystemManager;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /** [com.intellij.util.net.ssl.ConfirmingTrustManager] in >= IC-2022.2 */
    private static class TrustManagerWithMySystemManagersField implements X509TrustManager {

        List<X509TrustManager> mySystemManagers;

        public TrustManagerWithMySystemManagersField(List<X509TrustManager> mySystemManagers){
            this.mySystemManagers = mySystemManagers;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {

        }
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
