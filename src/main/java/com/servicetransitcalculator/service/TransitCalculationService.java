package com.servicetransitcalculator.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;

public interface TransitCalculationService {
    void processCsv(MultipartFile file);
    File getProcessedCsv();
}
