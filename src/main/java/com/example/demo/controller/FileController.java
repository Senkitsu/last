package com.example.demo.controller;

import java.io.IOException;
import java.lang.module.ResolutionException;

import org.apache.catalina.connector.Response;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.BusService;
import com.example.demo.service.FileService;
import com.example.demo.service.ProductService;

import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor
@RestController
public class FileController {
    
    private final FileService fileService;
    private final ProductService productService;
    @PostMapping(value = "/upload/{id}",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            String resultFile = fileService.storeFile(file);
            productService.addFileToProduct(id, resultFile);
                return ResponseEntity.ok(resultFile);
        } catch (IOException e) {
            return (ResponseEntity<?>) ResponseEntity.badRequest();
        } 

        }
    }