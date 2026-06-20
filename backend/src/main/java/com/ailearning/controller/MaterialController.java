package com.ailearning.controller;

import com.ailearning.common.ApiResponse;
import com.ailearning.dto.CreateMaterialTextRequest;
import com.ailearning.dto.MaterialResponse;
import com.ailearning.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping("/text")
    public ApiResponse<MaterialResponse> createText(@Valid @org.springframework.web.bind.annotation.RequestBody CreateMaterialTextRequest request) {
        return ApiResponse.ok(materialService.createText(request.title(), request.content()));
    }

    @PostMapping("/files")
    public ApiResponse<MaterialResponse> createFile(@RequestParam(required = false) String title, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(materialService.createFile(title, file));
    }
}
