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

import com.intellij.openapi.diagnostic.Logger;
import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConfigWatcher implements Runnable {

    private static final Logger LOG = Logger.getInstance(ConfigWatcher.class);

    private final List<Path> configs;
    protected final Listener listener;
    private final HighSensitivityRegistrar registrar;
    private WatchService service;

    public interface Listener {
        void onUpdate(ConfigWatcher source, Config config);
    }

    public ConfigWatcher(String config, Listener listener) {
        this(Paths.get(config), listener);
    }

    public ConfigWatcher(Path config, Listener listener) {
        this(List.of(config), listener);
    }

    public ConfigWatcher(List<Path> configs, Listener listener) {
        this(configs, listener, new HighSensitivityRegistrar());
    }

    public ConfigWatcher(List<Path> configs, Listener listener, HighSensitivityRegistrar registrar) {
        this.configs = configs;
        this.listener = listener;
        this.registrar = registrar;
    }

    @Override
    public void run() {
        watch((Config config) -> listener.onUpdate(this, config));
    }

    public void close() throws IOException {
        if (service != null) {
            service.close();
        }
    }

    private void watch(Consumer<Config> consumer) {
        try (WatchService service = createWatchService()) {
            Collection<Path> watchedDirectories = getWatchedDirectories();
            watchedDirectories.forEach(directory ->
                new ConfigDirectoryWatch(directory, consumer, service, registrar).start()
            );
        } catch (IOException e) {
            String configPaths = configs.stream()
                .map(path -> path.toAbsolutePath().toString())
                .collect(Collectors.joining());
            Logger.getInstance(ConfigWatcher.class).warn(
                "Could not watch kubernetes config file at " + configPaths, e);
        }
    }

    protected WatchService createWatchService() throws IOException {
        return this.service = FileSystems.getDefault().newWatchService();
    }

    private Collection<Path> getWatchedDirectories() {
        return configs.stream()
                .filter(this::isFileInDirectory)
                .map(Path::getParent)
                .collect(Collectors.toSet());
    }

    protected boolean isFileInDirectory(Path path) {
        return path != null
        && Files.isRegularFile(path)
        && Files.isDirectory(path.getParent());
    }

    private class ConfigDirectoryWatch {
        private final Path directory;
        private final WatchService service;
        private final HighSensitivityRegistrar registrar;
        private final Consumer<Config> consumer;

        private ConfigDirectoryWatch(Path directory, Consumer<Config> consumer, WatchService service, HighSensitivityRegistrar registrar) {
            this.directory = directory;
            this.consumer = consumer;
            this.service = service;
            this.registrar = registrar;
        }

        private void start() {
            try {
                register(directory, service, registrar);
                watch(consumer, service);
            } catch (InterruptedException e) {
                LOG.warn("Watching " + directory + " was interrupted", e);
            } catch (IOException e) {
                LOG.warn("Could not watch " + directory, e);
            }
        }

        private void register(Path path, WatchService service, HighSensitivityRegistrar registrar) throws IOException {
            registrar.registerService(path,
                new WatchEvent.Kind[]{
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
                },
                service);
        }

        private void watch(Consumer<Config> consumer, WatchService service) throws InterruptedException {
            for (WatchKey key = service.take(); key != null; key = service.take()) {
                key.pollEvents().forEach((event) -> {
                    Path changed = getAbsolutePath(directory, (Path) event.context());
                    if (isConfigPath(changed)) {
                        consumer.accept(loadConfig(changed));
                    }
                });
                key.reset();
            }
        }

        protected boolean isConfigPath(Path path) {
            return configs != null
                    && configs.contains(path);
        }

        /**
         * Returns {@link Config} for the given path if the kube config file
         * <ul>
         *     <li>exists and</li>
         *     <li>is not empty and</li>
         *     <li>is valid yaml</li>
         * </ul>
         * Returns {@code null} otherwise.
         *
         * @param path the path to the kube config
         * @return returns true if the kube config that the event points to exists, is not empty and is valid yaml
         */
        private Config loadConfig(Path path) {
            // TODO: replace by Config#getKubeConfigFiles once kubernetes-client 7.0 is available
            if (path == null) {
                return null;
            }
            try {
                if (Files.exists(path)
                        && Files.size(path) > 0) {
                    return KubeConfigUtils.parseConfig(path.toFile());
                }
            } catch (Exception e) {
                // only catch
                LOG.warn("Could not load kube config at " + path.toAbsolutePath(), e);
            }
            return null;
        }

        private Path getAbsolutePath(Path directory, Path relativePath) {
            return directory.resolve(relativePath);
        }

    }

}
