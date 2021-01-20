package com.redhat.devtools.intellij.common.telemetry;

public class TelemetryEvent implements ITelemetryEvent {

    private final Type type;
    private final String name;

    public TelemetryEvent(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }
}
