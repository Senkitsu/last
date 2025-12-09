package com.example.demo.dto;

import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CsvImportResult {
    private int  successCount;
    private int failedCounts;
    private List<String> errors;
    
    public boolean hasError()
    {
        return !errors.isEmpty();
    }
}
