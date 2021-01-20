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

import com.segment.analytics.Analytics;
import com.segment.analytics.messages.Message;
import com.segment.analytics.messages.MessageBuilder;
import com.segment.analytics.messages.TrackMessage;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static com.redhat.devtools.intellij.common.telemetry.ITelemetryEvent.Type.TRACK;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SegmentMessageBrokerTest {

    private static final String EXTENSION_ID = "com.redhat.devtools.intellij.common.telemetry.TelemetryServiceTest";
    private static final String USER_ID = "papa-smurf";
    private static final ITelemetryEvent EVENT_TRACK = new TelemetryEvent(TRACK, "Testing Telemetry");

    private Analytics analytics;
    private SegmentMessageBroker broker;

    @Before
    public void before() {
        this.analytics = createAnalytics();
        this.broker = new SegmentMessageBroker(EXTENSION_ID, analytics, USER_ID);
    }

    @Test
    public void should_enqueue_track_message() {
        // given
        // when
        broker.send(EVENT_TRACK);
        // then
        verify(analytics).enqueue(isA(TrackMessage.Builder.class));
    }

    @Test
    public void should_enqueue_track_message_with_anonymousId_and_userId() {
        // given
        ArgumentCaptor<MessageBuilder<?,?>> builder = ArgumentCaptor.forClass(MessageBuilder.class);
        // when
        broker.send(EVENT_TRACK);
        // then
        verify(analytics).enqueue(builder.capture());
        Message message = builder.getValue().build();
        assertThat(message.userId()).isEqualTo(USER_ID);
        assertThat(message.anonymousId()).isEqualTo(EXTENSION_ID);
    }

    private Analytics createAnalytics() {
        return mock(Analytics.class);
    }
}
