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
package com.redhat.devtools.intellij.common.telemetry;

import com.intellij.openapi.diagnostic.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TelemetryService implements ITelemetryService {

    private static final Logger LOGGER = Logger.getInstance(TelemetryService.class);

    private final IMessageBroker broker;
    private final Queue onHold = new ConcurrentLinkedQueue();

    public TelemetryService(String extensionId) {
        this(new SegmentMessageBroker(extensionId));
    }

    public TelemetryService(IMessageBroker broker) {
        this.broker = broker;
    }

    public boolean isActive() {
        return true;
    }

    public void send(ITelemetryEvent event) {
        broker.send(event);
    }
}
