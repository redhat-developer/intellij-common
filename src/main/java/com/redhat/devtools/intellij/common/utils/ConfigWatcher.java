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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.function.Consumer;

public class ConfigWatcher implements Runnable {

    private static final Logger LOG = Logger.getInstance(ConfigWatcher.class);

    private final Path config;
    protected Listener listener;

    public interface Listener {
        void onUpdate(ConfigWatcher source, Config config);
    }

    public ConfigWatcher(String config, Listener listener) {
        this(Paths.get(config), listener);
    }

    public ConfigWatcher(Path config, Listener listener) {
        this.config = config;
        this.listener = listener;
    }

    @Override
    public void run() {
        runOnConfigChange((Config config) -> {
            if (config != null) {
                listener.onUpdate(this, config);
            }
        });
    }

    protected Config loadConfig() {
        try {
            return ConfigHelper.loadKubeConfig(config.toAbsolutePath().toString());
        } catch (IOException e) {
            return null;
        }
    }

    private void runOnConfigChange(Consumer<Config> consumer) {
        try (WatchService service = newWatchService()) {
            registerWatchService(service);
            WatchKey key;
            while ((key = service.take()) != null) {
                key.pollEvents().stream()
                        .forEach((event) -> consumer.accept(loadConfig(getPath(event))));
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            Logger.getInstance(ConfigWatcher.class).warn(
                    "Could not watch kubernetes config file at " + config.toAbsolutePath(), e);
        }
    }

    protected WatchService newWatchService() throws IOException {
        return FileSystems.getDefault().newWatchService();
    }

    @NotNull
    private void registerWatchService(WatchService service) throws IOException {
        HighSensitivityRegistrar modifier = new HighSensitivityRegistrar();
        modifier.registerService(getWatchedPath(),
                new WatchEvent.Kind[]{
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE},
                service);
    }

    protected boolean isConfigPath(Path path) {
        return path.equals(config);
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
        if (path == null) {
            return null;
        }
        try {
            if (Files.exists(path)
                    && isConfigPath(path)
                    && Files.size(path) > 0) {
                return KubeConfigUtils.parseConfig(path.toFile());
            }
        } catch (Exception e) {
            // only catch
            LOG.warn("Could not load kube config at " + path.toAbsolutePath(), e);
        }
        return null;
    }

    private Path getPath(WatchEvent<?> event) {
        return getWatchedPath().resolve((Path) event.context());
    }

    private Path getWatchedPath() {
        return config.getParent();
    }

}
