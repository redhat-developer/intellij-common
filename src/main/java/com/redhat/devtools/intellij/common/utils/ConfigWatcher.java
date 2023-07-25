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

public class ConfigWatcher implements Runnable {

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
        runOnConfigChange(() -> {
            Config config = loadConfig();
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

    private void runOnConfigChange(Runnable runnable) {
        try (WatchService service = newWatchService()) {
            registerWatchService(service);
            WatchKey key;
            while ((key = service.take()) != null) {
                key.pollEvents().stream()
                        .filter(this::isConfigPath)
                        .filter(this::isValid)
                        .forEach(Void -> runnable.run());
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

    protected boolean isConfigPath(WatchEvent<?> event) {
        Path path = getWatchedPath().resolve((Path) event.context());
        return path.equals(config);
    }

	/**
	 * Returns {@code true} if the path (to the kube config file) in the given event
	 * <ul>
	 *     <li>exists and</li>
	 *     <li>is not empty</li>
	 * </ul>
	 *
	 * @param event the WatchEvent to get the path to the kube config from
	 * @return returns true if the kube config that the event points to exists and is not empty
	 */
	protected boolean isValid(WatchEvent<?> event) {
		Path path = getWatchedPath().resolve((Path) event.context());
		try {
			return Files.exists(path) && Files.size(path) > 0;
		} catch (IOException e) {
			// do nothing
		}
		return true;
	}

    private Path getWatchedPath() {
        return config.getParent();
    }

}
