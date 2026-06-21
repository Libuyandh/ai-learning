package com.ailearning.service;

import com.ailearning.common.BusinessException;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentParser {
    public String parse(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".markdown")) {
                return new String(file.getBytes());
            }
            if (name.endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                    return new PDFTextStripper().getText(document);
                }
            }
            if (name.endsWith(".docx")) {
                try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
                    return document.getParagraphs().stream().map(XWPFParagraph::getText).reduce("", (left, right) -> left + "\n" + right);
                }
            }
        } catch (IOException exception) {
            throw new BusinessException("MATERIAL_PARSE_FAILED", "资料解析失败", HttpStatus.BAD_REQUEST);
        }
        throw new BusinessException("UNSUPPORTED_FILE", "仅支持 PDF、DOCX、TXT、MD", HttpStatus.BAD_REQUEST);
    }
}
