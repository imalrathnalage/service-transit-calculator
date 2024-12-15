package com.servicetransitcalculator.service.impl;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.servicetransitcalculator.model.Tap;
import com.servicetransitcalculator.model.Trip;
import com.servicetransitcalculator.service.TransitCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TransitCalculationServiceImpl implements TransitCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(TransitCalculationServiceImpl.class);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // Thread-safe structure for dynamic fare configuration
    private final Map<String, Double> fareMap = new ConcurrentHashMap<>();

    // Thread-safe storage for processed trips
    private final List<Trip> trips = Collections.synchronizedList(new ArrayList<>());

    public TransitCalculationServiceImpl() {
        // Initial fares; can be replaced by dynamic loading from a config or database
        loadFareData();
    }

    private void loadFareData() {
        fareMap.put("Stop1-Stop2", 3.25);
        fareMap.put("Stop2-Stop3", 5.50);
        fareMap.put("Stop1-Stop3", 7.30);
        // Add more stops dynamically in the future
    }

    @Override
    public void processCsv(MultipartFile file) {
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> records = csvReader.readAll();

            if (records.isEmpty()) {
                logger.error("CSV file is empty.");
                throw new IllegalArgumentException("CSV file is empty.");
            }

            List<Tap> taps = records.stream()
                    .skip(1) // Skip the header
                    .map(this::parseTap)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            taps.stream()
                    .collect(Collectors.groupingBy(Tap::getPan))
                    .forEach(this::processTapsForPan);

        } catch (IllegalArgumentException e) {
            // Retain specific exception for empty file
            throw e;
        } catch (Exception e) {
            logger.error("Error processing CSV file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process CSV file.", e);
        }
    }

    public Tap parseTap(String[] record) {
        logger.debug("Parsing record: {}", Arrays.toString(record));
        if (record.length != 7) {
            logger.warn("Invalid record length: {}", Arrays.toString(record));
            return null;
        }

        try {
            LocalDateTime dateTime = parseDateTime(record[1]); // Use helper method to parse the date
            return new Tap(
                    Long.parseLong(record[0]),
                    dateTime,
                    Tap.TapType.fromString(record[2]),
                    record[3],
                    record[4],
                    record[5],
                    record[6]
            );
        } catch (Exception e) {
            logger.warn("Failed to parse record: {} due to error: {}", Arrays.toString(record), e.getMessage());
            return null;
        }
    }

    // Helper method to handle multiple date formats
    private LocalDateTime parseDateTime(String dateString) {
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(dateString, formatter);
            } catch (DateTimeParseException e) {
                // Continue trying other formats
            }
        }

        throw new DateTimeParseException("Date does not match any known formats", dateString, 0);
    }

    private void processTapsForPan(String pan, List<Tap> taps) {
        taps.sort(Comparator.comparing(Tap::getDateTimeUtc));

        for (int i = 0; i < taps.size(); i++) {
            Tap tapOn = taps.get(i);

            if (i + 1 < taps.size() && tapOn.getTapType() == Tap.TapType.ON) {
                Tap tapOff = taps.get(i + 1);

                // Skip duplicate taps or taps with the same stop ID
                if (tapOff.getTapType() == Tap.TapType.OFF && !tapOn.getStopId().equals(tapOff.getStopId())) {
                    createTrip(tapOn, tapOff, true);
                    i++; // Skip the processed OFF tap
                } else {
                    createTrip(tapOn, null, false);
                }
            } else {
                createTrip(tapOn, null, false);
            }
        }
    }

    public void createTrip(Tap tapOn, Tap tapOff, boolean isCompleted) {
        String tripKey = isCompleted ? tapOn.getStopId() + "-" + tapOff.getStopId() : null;
        double fare = isCompleted
                ? fareMap.getOrDefault(tripKey, 0.0)
                : fareMap.values().stream().max(Double::compare).orElse(0.0);

        if (isCompleted && fare == 0.0) {
            logger.warn("No fare mapping found for trip from {} to {}. Defaulting to $0.00.", tapOn.getStopId(), tapOff.getStopId());
        }

        trips.add(new Trip(
                tapOn.getDateTimeUtc().toString(),
                isCompleted ? tapOff.getDateTimeUtc().toString() : null,
                isCompleted ? calculateDuration(tapOn.getDateTimeUtc(), tapOff.getDateTimeUtc()) : 0,
                tapOn.getStopId(),
                isCompleted ? tapOff.getStopId() : "N/A",
                String.format("$%.2f", fare),
                tapOn.getCompanyId(),
                tapOn.getBusId(),
                tapOn.getPan(),
                isCompleted ? "COMPLETED" : "INCOMPLETE"
        ));
    }

    public long calculateDuration(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toSeconds();
    }

    @Override
    public File getProcessedCsv() {
        File file = new File("trips.csv");
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(file))) {
            csvWriter.writeNext(new String[]{
                    "Started", "Finished", "DurationSecs", "FromStopId", "ToStopId", "ChargeAmount", "CompanyId", "BusId", "PAN", "Status"
            });

            synchronized (trips) {
                trips.forEach(trip -> csvWriter.writeNext(new String[]{
                        trip.getStarted(),
                        trip.getFinished(),
                        String.valueOf(trip.getDurationSecs()),
                        trip.getFromStopId(),
                        trip.getToStopId(),
                        trip.getChargeAmount(),
                        trip.getCompanyId(),
                        trip.getBusId(),
                        trip.getPan(),
                        trip.getStatus()
                }));
            }
        } catch (IOException e) {
            logger.error("Error writing processed CSV file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate CSV file.", e);
        }
        return file;
    }

    public List<Trip> getTrips() {
        synchronized (trips) {
            return new ArrayList<>(trips); // Return a copy to prevent external modifications
        }
    }

}