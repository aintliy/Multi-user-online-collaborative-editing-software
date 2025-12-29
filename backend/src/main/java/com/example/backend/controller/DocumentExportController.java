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
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.ApiResponse;
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
     * 导出文档为Word格式
     */
    @GetMapping("/{id}/export/word")
    public ApiResponse<ResponseEntity<byte[]>> exportToWord(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long id) throws Exception {
        User user = userService.getUserByEmail(userDetails.getUsername());
        byte[] content = documentExportService.exportToWord(id, user.getId());
        
        String filename = URLEncoder.encode("document.docx", StandardCharsets.UTF_8);
        return ApiResponse.success("导出成功",
                ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(content)
        );
    }
    
    /**
     * 导出文档为PDF格式
     */
    @GetMapping("/{id}/export/pdf")
    public ApiResponse<ResponseEntity<byte[]>> exportToPdf(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable Long id) throws Exception {
        User user = userService.getUserByEmail(userDetails.getUsername());
        byte[] content = documentExportService.exportToPdf(id, user.getId());
        
        String filename = URLEncoder.encode("document.pdf", StandardCharsets.UTF_8);
        return ApiResponse.success("导出成功",
                ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content)
        );
    }
    
    /**
     * 导出文档为纯文本格式
     */
    @GetMapping("/{id}/export/txt")
    public ApiResponse<ResponseEntity<byte[]>> exportToText(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        byte[] content = documentExportService.exportToText(id, user.getId());
        
        String filename = URLEncoder.encode("document.txt", StandardCharsets.UTF_8);
        return ApiResponse.success("导出成功",
                ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content)
        );
    }
    
    /**
     * 导出文档为Markdown格式
     */
    @GetMapping("/{id}/export/md")
    public ApiResponse<ResponseEntity<byte[]>> exportToMarkdown(@AuthenticationPrincipal UserDetails userDetails,
                                                    @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        byte[] content = documentExportService.exportToMarkdown(id, user.getId());
        
        String filename = URLEncoder.encode("document.md", StandardCharsets.UTF_8);
        return ApiResponse.success("导出成功",
                ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content)
        );
    }
}
