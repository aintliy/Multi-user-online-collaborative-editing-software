package com.example.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import com.example.backend.entity.Document;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.DocumentCollaboratorRepository;
import com.example.backend.repository.DocumentRepository;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档导出服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentExportService {
    
    private final DocumentRepository documentRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    private final FileStorageService fileStorageService;
    
    /**
     * 导出为Word文档
     */
    public byte[] exportToWord(Long documentId, Long userId) throws IOException {
        Document document = getDocumentWithAccess(documentId, userId);
        
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // 添加标题
            XWPFParagraph titleParagraph = doc.createParagraph();
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(document.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            
            // 添加内容
            if (document.getContent() != null && !document.getContent().isEmpty()) {
                String[] paragraphs = document.getContent().split("\n");
                for (String para : paragraphs) {
                    XWPFParagraph paragraph = doc.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(para);
                }
            }
            
            doc.write(out);
            byte[] exportedBytes = out.toByteArray();
            
            // 保存导出的文件到文档存储目录
            if (document.getStoragePath() != null) {
                try {
                    String fileName = sanitizeFileName(document.getTitle()) + ".docx";
                    fileStorageService.saveBytes(document.getStoragePath(), fileName, exportedBytes);
                    log.debug("Word文件已保存到存储目录: {}", document.getStoragePath() + fileName);
                } catch (Exception e) {
                    log.warn("保存Word文件到存储目录失败", e);
                    // 不影响导出流程
                }
            }
            
            return exportedBytes;
        }
    }
    
    /**
     * 导出为PDF文档
     */
    public byte[] exportToPdf(Long documentId, Long userId) throws DocumentException, IOException {
        Document document = getDocumentWithAccess(documentId, userId);
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document pdfDoc = new com.lowagie.text.Document();
            PdfWriter.getInstance(pdfDoc, out);
            
            pdfDoc.open();
            
            // 添加标题
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            pdfDoc.add(new Paragraph(document.getTitle(), titleFont));
            pdfDoc.add(new Paragraph(" ")); // 空行
            
            // 添加内容
            if (document.getContent() != null && !document.getContent().isEmpty()) {
                String[] paragraphs = document.getContent().split("\n");
                for (String para : paragraphs) {
                    pdfDoc.add(new Paragraph(para));
                }
            }
            
            pdfDoc.close();
            byte[] exportedBytes = out.toByteArray();
            
            // 保存导出的文件到文档存储目录
            if (document.getStoragePath() != null) {
                try {
                    String fileName = sanitizeFileName(document.getTitle()) + ".pdf";
                    fileStorageService.saveBytes(document.getStoragePath(), fileName, exportedBytes);
                    log.debug("PDF文件已保存到存储目录: {}", document.getStoragePath() + fileName);
                } catch (Exception e) {
                    log.warn("保存PDF文件到存储目录失败", e);
                    // 不影响导出流程
                }
            }
            
            return exportedBytes;
        }
    }
    
    /**
     * 导出为纯文本
     */
    public byte[] exportToText(Long documentId, Long userId) {
        Document document = getDocumentWithAccess(documentId, userId);
        
        StringBuilder sb = new StringBuilder();
        sb.append(document.getTitle()).append("\n\n");
        if (document.getContent() != null) {
            sb.append(document.getContent());
        }
        
        return sb.toString().getBytes();
    }
    
    /**
     * 导出为Markdown
     */
    public byte[] exportToMarkdown(Long documentId, Long userId) {
        Document document = getDocumentWithAccess(documentId, userId);
        
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(document.getTitle()).append("\n\n");
        if (document.getContent() != null) {
            sb.append(document.getContent());
        }
        
        return sb.toString().getBytes();
    }
    
    /**
     * 获取有访问权限的文档
     */
    private Document getDocumentWithAccess(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在"));
        if (isDeleted(document)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在或已被删除");
        }
        
        // 检查访问权限
        boolean isOwner = document.getOwner().getId().equals(userId);
        boolean isPublic = "public".equals(document.getVisibility());
        boolean isCollaborator = collaboratorRepository.existsByDocumentIdAndUserId(documentId, userId);
        
        if (!isOwner && !isPublic && !isCollaborator) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "无权访问此文档");
        }
        
        return document;
    }

    private boolean isDeleted(Document document) {
        return document == null || "deleted".equalsIgnoreCase(document.getStatus())
                || document.getFolder() == null || document.getFolder().getParent() == null;
    }
    
    /**
     * 清理文件名，移除不安全字符
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "document";
        }
        // 移除或替换不安全的文件系统字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
