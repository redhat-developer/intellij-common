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
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.Message;
import com.segment.analytics.messages.MessageBuilder;
import com.segment.analytics.messages.PageMessage;
import com.segment.analytics.messages.TrackMessage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class SegmentMessageBroker implements IMessageBroker {

    private static final Logger LOGGER = Logger.getInstance(SegmentMessageBroker.class);

    private final String extensionId;
    private final Analytics analytics;
    private final String userId;
    private AtomicBoolean identified = new AtomicBoolean(false);

    public SegmentMessageBroker(String extensionId) {
        this(extensionId, AnalyticsFactory.INSTANCE.create(), RedHatUUID.INSTANCE.get());
    }

    public SegmentMessageBroker(String extensionId, Analytics analytics, String userId) {
        this.extensionId = extensionId;
        this.analytics = analytics;
        this.userId = userId;
    }

    @Override
    public void send(ITelemetryEvent event) {
        if (analytics == null) {
            LOGGER.warn("Could not send " + event.getType() + " event '" + event.getName() + "': no analytics instance present.");
            return;
        }

        MessageBuilder builder = toMessage(event);
        LOGGER.debug("Sending message " + builder.type() + " to segment." );
        analytics.enqueue(builder);
    }

    private MessageBuilder toMessage(ITelemetryEvent event) {
        MessageBuilder builder = messageBuilder(event);
        return builder
                .userId(userId)
                .anonymousId(extensionId);
    }

    @NotNull
    private MessageBuilder<? extends Message, ? extends MessageBuilder> messageBuilder(ITelemetryEvent event) {
        switch (event.getType()) {
            case IDENTIFY:
                return IdentifyMessage.builder();
            default:
            case TRACK:
                return TrackMessage.builder(event.getName());
            case PAGE:
                return PageMessage.builder(event.getName());
        }
    }

    public void shutdown() {
        analytics.flush();
        analytics.shutdown();
    }

    public String getExtensionId() {
        return extensionId;
    }

}
