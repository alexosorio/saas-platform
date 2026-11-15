package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = uploadService.uploadProductImage(file);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
