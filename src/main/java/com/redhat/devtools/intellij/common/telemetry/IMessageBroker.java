package com.redhat.devtools.intellij.common.telemetry;

public interface IMessageBroker {
    void send(ITelemetryEvent event);
}
