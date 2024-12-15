package com.servicetransitcalculator.controller;

import com.servicetransitcalculator.service.TransitCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * Controller for handling transit calculation endpoints.
 */
@RestController
@RequestMapping("/transit")
public class TransitCalculatorController {

    private static final Logger logger = LoggerFactory.getLogger(TransitCalculatorController.class);

    private final TransitCalculationService service;

    public TransitCalculatorController(TransitCalculationService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadCsv(@RequestParam("file") MultipartFile file) {
        logger.info("Request received: Upload CSV file '{}'", file.getOriginalFilename());

        if (file.isEmpty()) {
            logger.warn("Uploaded file '{}' is empty.", file.getOriginalFilename());
            throw new IllegalArgumentException("File is empty. Please upload a valid CSV file.");
        }

        service.processCsv(file);
        logger.info("CSV file '{}' processed successfully.", file.getOriginalFilename());
        return ResponseEntity.ok("CSV processed successfully.");
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadCsv() {
        logger.info("Request received: Download processed CSV file.");

        File file = service.getProcessedCsv();

        if (file == null || !file.exists()) {
            logger.warn("No processed file available for download.");
            throw new IllegalArgumentException("No processed file available for download.");
        }

        logger.info("Processed file '{}' ready for download.", file.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .body(new FileSystemResource(file));
    }
}