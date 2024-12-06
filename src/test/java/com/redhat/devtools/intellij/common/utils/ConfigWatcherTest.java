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

import io.fabric8.kubernetes.client.Config;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigWatcherTest {

    private Path config1;
    private Path config2;
    private Path config3;

    private final ConfigWatcher.Listener listener = mock(ConfigWatcher.Listener.class);
    private final HighSensitivityRegistrar registrar = Mockito.mock(HighSensitivityRegistrar.class);
    private final WatchService service =  mock(WatchService.class);

    private ConfigWatcher watcher;

    @Before
    public void before() throws IOException {
        this.config1 = Paths.get(Files.createTempFile("skywalker", null).toString());
        this.config2 = Paths.get(Files.createTempFile("yoda", null).toString());
        this.config3 = Paths.get(Files.createTempFile("obiwan", null).toString());
        this.watcher = new TestableConfigWatcher(List.of(config1, config2, config3), listener, registrar, service);
    }

    @Test
    public void run_registers_service_for_parent_directory() throws IOException {
        // given
        // when
        watcher.run();
        // then
        verify(registrar).registerService(eq(config1.getParent()), any(), any());
        verify(registrar).registerService(eq(config2.getParent()), any(), any());
        verify(registrar).registerService(eq(config3.getParent()), any(), any());
    }

    @Test
    public void run_registers_service_only_once_if_parent_directory_is_the_same_for_all_configs() throws IOException {
        // given
        // when
        watcher.run();
        // then
        verify(registrar, times(1)).registerService(any(), any(), any());
    }

    @Test
    public void run_does_NOT_register_service_if_config_is_directory() throws IOException {
        // given
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        ConfigWatcher watcher = new TestableConfigWatcher(List.of(tempDir), listener, registrar, service);
        // when
        watcher.run();
        // then
        verify(registrar, never()).registerService(any(), any(), any());
    }

    @Test
    public void listener_is_called_if_config_file_is_changed() throws InterruptedException {
        // given
        ReportingListener reportingListener = new ReportingListener();
        ConfigWatcher watcher = new TestableConfigWatcher(List.of(config1), reportingListener, registrar, service);
        createWatchKeyForService(config1, service); // config-file
        // when
        watcher.run();
        // then
        assertThat(reportingListener.isCalled()).isTrue();
    }

    @Test
    public void listener_is_NOT_called_if_a_different_file_is_changed() throws InterruptedException {
        // given
        ReportingListener reportingListener = new ReportingListener();
        ConfigWatcher watcher = new TestableConfigWatcher(List.of(config1), reportingListener, registrar, service);
        createWatchKeyForService(config2, service); // non-config file
        // when
        watcher.run();
        // then
        assertThat(reportingListener.isCalled()).isFalse();
    }

    @Test
    public void listener_resets_key_after_consuming_its_events() throws InterruptedException {
        // given
        ReportingListener reportingListener = new ReportingListener();
        ConfigWatcher watcher = new TestableConfigWatcher(List.of(config1), reportingListener, registrar, service);
        WatchKey key = createWatchKeyForService(config2, service); // non-config file
        // when
        watcher.run();
        // then
        verify(key).reset();
    }

    @Test
    public void close_is_closing_service_if_it_was_run() throws IOException {
        // given
        watcher.run();
        // when
        watcher.close();
        // then
        verify(service).close();
    }

    @Test
    public void close_is_NOT_closing_service_if_it_was_NOT_run() throws IOException {
        // given
        // when
        watcher.close();
        // then
        verify(service, never()).close();
    }

    private static WatchKey createWatchKeyForService(Path path, WatchService service) throws InterruptedException {
        WatchEvent<Path> event = mock(WatchEvent.class);
        when(event.context())
                .thenReturn(path);
        WatchKey key = mock(WatchKey.class);
        when(key.pollEvents())
                .thenReturn(List.of(event));
        when(service.take())
                .thenReturn(key)
                .thenReturn(null); // 2nd call, causes listener to stop
        return key;
    }

    private static class ReportingListener implements ConfigWatcher.Listener {

        private boolean called = false;

        @Override
        public void onUpdate(Config updatedConfig) {
            this.called = true;
        }

        public boolean isCalled() throws InterruptedException {
            return called;
        }
    }

    private static class TestableConfigWatcher extends ConfigWatcher {

        private final WatchService service;

        public TestableConfigWatcher(List<Path> configs, Listener listener, HighSensitivityRegistrar registrar, WatchService service) {
            super(configs, listener, registrar);
            this.service = service;
        }

        @Override
        protected WatchService createWatchService() throws IOException {
            return service;
        }
    }
}
