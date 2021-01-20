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

import com.jakewharton.retrofit.Ok3Client;
import com.redhat.devtools.intellij.common.telemetry.util.BlockingFlush;
import com.redhat.devtools.intellij.common.telemetry.util.StdOutLogging;
import com.segment.analytics.Analytics;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit.client.Client;

import java.util.concurrent.TimeUnit;

import static com.redhat.devtools.intellij.common.telemetry.ITelemetryEvent.Type.TRACK;

public class TelemetryServiceIntegrationTest {

    private static final String extensionId = "com.redhat.devtools.intellij.common.telemetry.TelemetryServiceTest";
    private static final TelemetryEvent EVENT_TRACK = new TelemetryEvent(TRACK, "Testing Telemetry");

    private BlockingFlush blockingFlush;
    private Analytics analytics;
    private ITelemetryService service;

    @Before
    public void before() {
        this.blockingFlush = BlockingFlush.create();
        Client client = createClient();
        this.analytics = createAnalytics(blockingFlush, client);
        this.service = new TelemetryService(new SegmentMessageBroker(extensionId, analytics, RedHatUUID.INSTANCE.get()));
    }

    @After
    public void after() {
        shutdownAnalytics();
    }

    private void shutdownAnalytics() {
        analytics.flush();
        blockingFlush.block();
        analytics.shutdown();
    }


    @Test
    public void should_send_track_event() {
        // given
        // when
        service.send(EVENT_TRACK);
        // then
    }

    private Analytics createAnalytics(BlockingFlush blockingFlush, Client client) {
        return Analytics.builder("HYuMCHlIpTvukCKZA42OubI1cvGIAap6")
                .plugin(new StdOutLogging())
                .plugin(blockingFlush.plugin())
                .client(client)
                .build();
    }

    private Client createClient() {
        return new Ok3Client(
                new OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                        .writeTimeout(5, TimeUnit.SECONDS)
                        .build());
    }
}
