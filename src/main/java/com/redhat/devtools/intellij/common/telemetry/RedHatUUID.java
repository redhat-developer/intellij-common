package com.redhat.devtools.intellij.common.telemetry;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.file.Files;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class RedHatUUID {

    private static final Logger LOGGER = Logger.getInstance(RedHatUUID.class);

    public static final RedHatUUID INSTANCE = new RedHatUUID();
    private static final Path REDHAT_DIRECTORY = Paths.get(System.getProperty("user.home"), ".redhat");
    private static final Path UUID_FILE = REDHAT_DIRECTORY.resolve("anonymousId");

    private String uuid;

    private RedHatUUID() {}

    public String get() {
        if (uuid == null) {
            if (Files.exists(UUID_FILE)) {
                this.uuid = load(UUID_FILE);
            } else {
                this.uuid = UUID.randomUUID().toString();
                write(uuid, REDHAT_DIRECTORY, UUID_FILE);
            }
        }
        return uuid;
    }

    private String load(Path uuidFile) {
        String uuid = null;
        try {
            uuid = Files.lines(uuidFile)
                    .findAny()
                    .map(String::trim)
                    .orElse(null);
        } catch (IOException e) {
            LOGGER.warn("Could not read redhat anonymous UUID file at " + uuidFile.toAbsolutePath(), e);
        }
        return uuid;
    }

    private void write(String uuid, Path directory, Path uuidFile) {
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
            Files.createFile(UUID_FILE);
            Files.newBufferedWriter(UUID_FILE)
                .append(uuid)
                .close();
        } catch (IOException e) {
            LOGGER.warn("Could not write redhat anonymous UUID to file at " + UUID_FILE.toAbsolutePath(), e);
        }
    }
}
