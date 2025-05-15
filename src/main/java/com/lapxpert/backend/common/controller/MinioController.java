package com.lapxpert.backend.common.controller;

import com.lapxpert.backend.common.service.MinioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage")
@CrossOrigin(origins = "*")
public class MinioController {
    private final MinioService minioService;

    public MinioController(MinioService minioService) {
        this.minioService = minioService;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<String>> upload(
            @RequestParam("bucket") String bucket,
            @RequestParam("files") MultipartFile[] files) {
        try {
            List<String> urls = minioService.uploadFile(bucket, files);
            return ResponseEntity.ok(urls);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
