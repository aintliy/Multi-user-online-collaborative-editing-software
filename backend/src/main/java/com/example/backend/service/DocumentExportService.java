package com.example.backend.service;

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
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 文档导出服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentExportService {
    
    private final DocumentRepository documentRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    
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
            return out.toByteArray();
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
            return out.toByteArray();
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
        
        // 检查访问权限
        boolean isOwner = document.getOwner().getId().equals(userId);
        boolean isPublic = "public".equals(document.getVisibility());
        boolean isCollaborator = collaboratorRepository.existsByDocumentIdAndUserId(documentId, userId);
        
        if (!isOwner && !isPublic && !isCollaborator) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "无权访问此文档");
        }
        
        return document;
    }
}
