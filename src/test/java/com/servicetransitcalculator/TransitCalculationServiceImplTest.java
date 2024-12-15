package com.servicetransitcalculator;

import com.servicetransitcalculator.model.Tap;
import com.servicetransitcalculator.service.impl.TransitCalculationServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.when;

class TransitCalculationServiceImplTest {

    private TransitCalculationServiceImpl service;

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @BeforeEach
    void setUp() {
        service = new TransitCalculationServiceImpl();
    }

    @Test
    void testParseTap_ValidTap() {
        // Ensure input matches the expected format for DATE_FORMATTER
        String[] validTap = {"1", "2024-01-01T12:00:00", "ON", "Stop1", "Company1", "Bus1", "123456"};
        Tap result = service.parseTap(validTap);

        // Check that the result is not null
        Assertions.assertNotNull(result, "Valid tap should not return null.");

        // Validate all fields of the parsed Tap object
        Assertions.assertEquals(1L, result.getId());
        Assertions.assertEquals(LocalDateTime.parse("2024-01-01T12:00:00"), result.getDateTimeUtc());
        Assertions.assertEquals(Tap.TapType.ON, result.getTapType());
        Assertions.assertEquals("Stop1", result.getStopId());
        Assertions.assertEquals("Company1", result.getCompanyId());
        Assertions.assertEquals("Bus1", result.getBusId());
        Assertions.assertEquals("123456", result.getPan());
    }

    @Test
    void testCreateTrip_NoFareMapping() {
        Tap tapOn = new Tap(1L, LocalDateTime.parse("2024-01-01T12:00:00", DATE_FORMATTER), Tap.TapType.ON, "Stop1", "Company1", "Bus1", "123456");
        Tap tapOff = new Tap(2L, LocalDateTime.parse("2024-01-01T12:15:00", DATE_FORMATTER), Tap.TapType.OFF, "Stop4", "Company1", "Bus1", "123456");

        service.createTrip(tapOn, tapOff, true);

        File csvFile = service.getProcessedCsv();
        Assertions.assertTrue(csvFile.exists(), "Processed CSV file should be created.");
        Assertions.assertTrue(
                service.getTrips().stream().anyMatch(t -> t.getChargeAmount().equals("$0.00")),
                "Trip with missing fare mapping should have $0.00 charge."
        );
    }

    @Test
    void testParseTap_InvalidTap() {
        String[] invalidTap = {"1", "InvalidDate", "ON", "Stop1", "Company1", "Bus1", "123456"};
        Tap result = service.parseTap(invalidTap);

        Assertions.assertNull(result, "Invalid tap should return null.");
    }

    @Test
    void testProcessCsv_ValidFile() throws Exception {
        String csvContent = """
                Id,DateTimeUTC,TapType,StopId,CompanyId,BusId,PAN
                1,2024-01-01T12:00:00,ON,Stop1,Company1,Bus1,123456
                2,2024-01-01T12:15:00,OFF,Stop2,Company1,Bus1,123456
                """;
        MultipartFile file = mockMultipartFile(csvContent);

        Assertions.assertDoesNotThrow(() -> service.processCsv(file));
    }

    @Test
    void testProcessCsv_EmptyFile() {
        MultipartFile file = mockMultipartFile("");

        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> service.processCsv(file));
        Assertions.assertEquals("CSV file is empty.", exception.getMessage());
    }

    @Test
    void testProcessCsv_InvalidFile() {
        String csvContent = """
                Id,DateTimeUTC,TapType,StopId,CompanyId,BusId,PAN
                1,InvalidDate,ON,Stop1,Company1,Bus1,123456
                """;
        MultipartFile file = mockMultipartFile(csvContent);

        Assertions.assertDoesNotThrow(() -> service.processCsv(file));
    }

    @Test
    void testProcessTapsForPan() {
        String csvContent = """
                Id,DateTimeUTC,TapType,StopId,CompanyId,BusId,PAN
                1,2024-01-01T12:00:00,ON,Stop1,Company1,Bus1,123456
                2,2024-01-01T12:15:00,OFF,Stop2,Company1,Bus1,123456
                3,2024-01-01T12:30:00,ON,Stop2,Company1,Bus1,123456
                """;
        MultipartFile file = mockMultipartFile(csvContent);
        Assertions.assertDoesNotThrow(() -> service.processCsv(file));
    }

    @Test
    void testCreateTrip() {
        Tap tapOn = new Tap(1L, LocalDateTime.parse("2024-01-01T12:00:00"), Tap.TapType.ON, "Stop1", "Company1", "Bus1", "123456");
        Tap tapOff = new Tap(2L, LocalDateTime.parse("2024-01-01T12:15:00"), Tap.TapType.OFF, "Stop2", "Company1", "Bus1", "123456");

        service.createTrip(tapOn, tapOff, true);

        File csvFile = service.getProcessedCsv();
        Assertions.assertTrue(csvFile.exists(), "Processed CSV file should be created.");
    }

    @Test
    void testCalculateDuration() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 1, 12, 15);

        long duration = service.calculateDuration(start, end);
        Assertions.assertEquals(900, duration, "Duration should be 900 seconds.");
    }

    @Test
    void testGetProcessedCsv() {
        File file = service.getProcessedCsv();

        Assertions.assertNotNull(file, "Processed CSV file should not be null.");
        Assertions.assertTrue(file.exists(), "Processed CSV file should exist.");
    }

    // Helper method to mock MultipartFile
    private MultipartFile mockMultipartFile(String content) {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        try {
            InputStream inputStream = new ByteArrayInputStream(content.getBytes());
            when(file.getInputStream()).thenReturn(inputStream);
        } catch (Exception e) {
            Assertions.fail("Failed to mock MultipartFile", e);
        }
        return file;
    }
}