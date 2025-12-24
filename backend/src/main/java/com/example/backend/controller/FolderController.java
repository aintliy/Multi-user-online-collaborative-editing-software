package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.folder.CreateFolderRequest;
import com.example.backend.dto.folder.FolderDTO;
import com.example.backend.dto.folder.UpdateFolderRequest;
import com.example.backend.entity.User;
import com.example.backend.service.FolderService;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件夹控制器
 */
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {
    
    private final FolderService folderService;
    private final UserService userService;
    
    /**
     * 获取文件夹树
     */
    @GetMapping
    public ApiResponse<List<FolderDTO>> getFolderTree(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<FolderDTO> folders = folderService.getFolderTree(user.getId());
        return ApiResponse.success(folders);
    }
    
    /**
     * 创建文件夹
     */
    @PostMapping
    public ApiResponse<FolderDTO> createFolder(@AuthenticationPrincipal UserDetails userDetails,
                                                @Valid @RequestBody CreateFolderRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        FolderDTO folder = folderService.createFolder(user.getId(), request);
        return ApiResponse.success("创建成功", folder);
    }
    
    /**
     * 重命名文件夹
     */
    @PutMapping("/{id}")
    public ApiResponse<FolderDTO> updateFolder(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long id,
                                                @Valid @RequestBody UpdateFolderRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        FolderDTO folder = folderService.updateFolder(id, user.getId(), request);
        return ApiResponse.success("更新成功", folder);
    }
    
    /**
     * 删除文件夹
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFolder(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        folderService.deleteFolder(id, user.getId(), isAdmin);
        return ApiResponse.success("删除成功");
    }
}
