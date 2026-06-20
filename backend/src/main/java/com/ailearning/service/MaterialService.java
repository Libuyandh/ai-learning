package com.ailearning.service;

import com.ailearning.common.BusinessException;
import com.ailearning.domain.Material;
import com.ailearning.dto.MaterialResponse;
import com.ailearning.mapper.MaterialMapper;
import com.ailearning.rag.RagStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MaterialService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private final MaterialMapper materialMapper;
    private final RagStore ragStore;
    private final DocumentParser documentParser;

    public MaterialService(MaterialMapper materialMapper, RagStore ragStore, DocumentParser documentParser) {
        this.materialMapper = materialMapper;
        this.ragStore = ragStore;
        this.documentParser = documentParser;
    }

    @Transactional
    public MaterialResponse createText(String title, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("VALIDATION_ERROR", "资料内容不能为空", HttpStatus.BAD_REQUEST);
        }
        return save(title, "text", null, content);
    }

    @Transactional
    public MaterialResponse createFile(String title, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "文件不能为空", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", "单个文件不能超过 10MB", HttpStatus.BAD_REQUEST);
        }
        String text = documentParser.parse(file);
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("MATERIAL_PARSE_FAILED", "资料解析为空", HttpStatus.BAD_REQUEST);
        }
        String fileName = file.getOriginalFilename();
        return save(StringUtils.hasText(title) ? title : fileName, "file", fileName, text);
    }

    private MaterialResponse save(String title, String type, String fileName, String text) {
        List<String> chunks = split(text);
        Material material = new Material();
        material.setTitle(StringUtils.hasText(title) ? title.trim() : "未命名资料");
        material.setType(type);
        material.setFileName(fileName);
        material.setStatus("READY");
        material.setChunkCount(chunks.size());
        material.setCreatedAt(LocalDateTime.now());
        materialMapper.insert(material);
        ragStore.add(material.getId(), chunks);
        return new MaterialResponse(material.getId(), material.getTitle(), material.getType(), material.getStatus(), material.getChunkCount());
    }

    private List<String> split(String text) {
        String normalized = text.trim().replaceAll("\\r\\n?", "\n");
        List<String> chunks = new ArrayList<>();
        int size = 900;
        int overlap = 120;
        for (int start = 0; start < normalized.length(); start += size - overlap) {
            int end = Math.min(start + size, normalized.length());
            String chunk = normalized.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }
            if (end == normalized.length()) {
                break;
            }
        }
        if (chunks.isEmpty()) {
            throw new BusinessException("MATERIAL_PARSE_FAILED", "资料解析为空", HttpStatus.BAD_REQUEST);
        }
        return chunks;
    }
}
