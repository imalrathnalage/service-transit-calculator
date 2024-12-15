package com.servicetransitcalculator.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

public class Tap {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final Long id;
    private final LocalDateTime dateTimeUtc;
    private final TapType tapType;
    private final String stopId;
    private final String companyId;
    private final String busId;
    private final String pan;

    public Tap(Long id, LocalDateTime dateTimeUtc, TapType tapType, String stopId, String companyId, String busId, String pan) {
        this.id = Objects.requireNonNull(id, "ID must not be null.");
        this.dateTimeUtc = Objects.requireNonNull(dateTimeUtc, "DateTime must not be null.");
        this.tapType = Objects.requireNonNull(tapType, "TapType must not be null.");
        this.stopId = Objects.requireNonNull(stopId, "Stop ID must not be null.");
        this.companyId = companyId;
        this.busId = busId;
        this.pan = pan;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDateTimeUtc() {
        return dateTimeUtc;
    }

    public TapType getTapType() {
        return tapType;
    }

    public String getStopId() {
        return stopId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getBusId() {
        return busId;
    }

    public String getPan() {
        return pan;
    }

    public enum TapType {
        ON, OFF;

        public static TapType fromString(String value) {
            return Arrays.stream(values())
                    .filter(type -> type.name().equalsIgnoreCase(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid TapType: " + value));
        }
    }
}