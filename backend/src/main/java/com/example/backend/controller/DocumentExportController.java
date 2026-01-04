package com.example.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.entity.User;
import com.example.backend.service.DocumentExportService;
import com.example.backend.service.UserService;

import lombok.RequiredArgsConstructor;



/**
 * 文档导出控制器
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentExportController {
    
    private final DocumentExportService documentExportService;
    private final UserService userService;
    
    /**
     * 导出文档为PDF格式
     */
    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable Long id,
                                               @RequestParam(required = false, defaultValue = "document") String filename) throws Exception {
        User user = userService.getUserByEmail(userDetails.getUsername());
        byte[] content = documentExportService.exportToPdf(id, user.getId());
        
        String encodedFilename = URLEncoder.encode(filename + ".pdf", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
    
    /**
     * 导出文档为纯文本格式
     */
    @GetMapping("/{id}/export/txt")
    public ResponseEntity<byte[]> exportToText(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long id,
                                                @RequestParam(required = false, defaultValue = "document") String filename) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        byte[] content = documentExportService.exportToText(id, user.getId());
        
        String encodedFilename = URLEncoder.encode(filename + ".txt", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(content);
    }
    
    /**
     * 导出文档为Markdown格式
     */
    @GetMapping("/{id}/export/md")
    public ResponseEntity<byte[]> exportToMarkdown(@AuthenticationPrincipal UserDetails userDetails,
                                                    @PathVariable Long id,
                                                    @RequestParam(required = false, defaultValue = "document") String filename) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        byte[] content = documentExportService.exportToMarkdown(id, user.getId());
        
        String encodedFilename = URLEncoder.encode(filename + ".md", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                .body(content);
    }
}
